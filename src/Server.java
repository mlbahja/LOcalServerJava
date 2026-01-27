package src;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
   private ConfigLoader.Config config;

    private Selector selector;  
    private Map<SocketChannel, ClientContext> clientContexts; 
    private Router router;  
    private ExecutorService cgiExecutor; 
    private volatile boolean running; 
    
    public Server(ConfigLoader.Config config) {
        this.config = config;
        this.clientContexts = new ConcurrentHashMap<>();
        this.router = new Router(config.getRoutes());
        this.cgiExecutor = Executors.newCachedThreadPool();
    }
    
    public void start() throws IOException {
        selector = Selector.open();
        running = true;
        
        System.out.println("Starting HTTP Server...");
        
        for (int port : config.getPorts()) {
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);  
            serverChannel.socket().bind(new InetSocketAddress(config.getHost(), port));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("  Listening on " + config.getHost() + ":" + port);
        }
        
        System.out.println("Server ready. Press Ctrl+C to stop.");
        
        while (running) {
            try {
                selector.select(config.getRequestTimeout());
                                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();
                
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();
                    
                    if (!key.isValid()) {
                        continue;
                    }
                    
                    if (key.isAcceptable()) {
                        acceptConnection(key);
                    } else if (key.isReadable()) {
                        readData(key);
                    } else if (key.isWritable()) {
                        writeData(key);
                    }
                }
                
                cleanupTimeoutConnections();
                
            } catch (IOException e) {
                System.err.println("Error in selector loop: " + e.getMessage());
                e.printStackTrace();
            } catch (ClosedSelectorException e) {
                break;
            }
        }
    }
    
    private void acceptConnection(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        
        ClientContext context = new ClientContext();
        context.startTime = System.currentTimeMillis();
        context.lastActivityTime = context.startTime;
        clientContexts.put(clientChannel, context);
        
        clientChannel.register(selector, SelectionKey.OP_READ, context);
        
        System.out.println("New connection from: " + clientChannel.getRemoteAddress());
    }
    
    private void readData(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ClientContext context = (ClientContext) key.attachment();
        
        ByteBuffer buffer = ByteBuffer.allocate(8192); // 8KB buffer
        int bytesRead;
        
        try {
            bytesRead = clientChannel.read(buffer);
        } catch (IOException e) {
            closeClient(clientChannel, key);
            return;
        }
        
        if (bytesRead == -1) {
            closeClient(clientChannel, key);
            return;
        }
        
        if (bytesRead > 0) {
            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            
            context.appendRequestData(data);
            context.lastActivityTime = System.currentTimeMillis();
            
            if (context.hasCompleteRequest()) {
                processRequest(clientChannel, context);
                key.interestOps(SelectionKey.OP_WRITE);
            }
        }
    }
    
    private void writeData(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ClientContext context = (ClientContext) key.attachment();
        
        if (context.responseData != null) {
            ByteBuffer buffer = ByteBuffer.wrap(context.responseData);
            
            try {
                int bytesWritten = clientChannel.write(buffer);
                
                if (bytesWritten > 0 && !buffer.hasRemaining()) {
                  
                    if (context.keepAlive) {
                        context.reset();
                        key.interestOps(SelectionKey.OP_READ);
                    } else {
                        closeClient(clientChannel, key);
                    }
                }
            } catch (IOException e) {
                closeClient(clientChannel, key);
            }
        }
    }
    
    private void processRequest(SocketChannel clientChannel, ClientContext context) {
        try {
            HttpRequest request = context.getHttpRequest();
            
            if (request.getBody().length > config.getClientBodySizeLimit()) {
                context.responseData = HttpResponse.errorResponse(
                    413, "Payload Too Large",
                    config.getErrorPages().get(413)
                ).build();
                return;
            }
            
            Router.RouteMatch match = router.match(request);
            
            if (match == null) {
                context.responseData = HttpResponse.errorResponse(
                    404, "Not Found",
                    config.getErrorPages().get(404)
                ).build();
                return;
            }
            
            switch (match.getType()) {
                case REDIRECT:
                    handleRedirect(context, match.getRoute());
                    break;
                case METHOD_NOT_ALLOWED:
                    context.responseData = HttpResponse.errorResponse(
                        405, "Method Not Allowed",
                        config.getErrorPages().get(405)
                    ).build();
                    break;
                case STATIC:
                    handleStaticFile(context, match.getRoute(), request);
                    break;
                case CGI:
                    handleCgiRequest(context, match.getRoute(), request);
                    break;
            }
            
        } catch (Exception e) {
            System.err.println("Error processing request: " + e.getMessage());
            e.printStackTrace();
            context.responseData = HttpResponse.errorResponse(
                500, "Internal Server Error",
                config.getErrorPages().get(500)
            ).build();
        }
    }
    
    private void handleRedirect(ClientContext context, ConfigLoader.Route route) {
        HttpResponse response = new HttpResponse();
        response.setStatus(301, "Moved Permanently");
        response.setHeader("Location", route.getRedirect());
        response.setBody("");
        context.responseData = response.build();
    }
    
    private void handleStaticFile(ClientContext context, ConfigLoader.Route route, HttpRequest request) {
        String filePath = router.resolveFilePath(route, request.getPath());
        
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(filePath);
            
            if (java.nio.file.Files.isDirectory(path)) {
                if (route.isDirectoryListing()) {
                    byte[] listing = router.generateDirectoryListing(
                        filePath, request.getPath()
                    );
                    if (listing != null) {
                        HttpResponse response = new HttpResponse();
                        response.setBody(listing);
                        context.responseData = response.build();
                    } else {
                        context.responseData = HttpResponse.errorResponse(
                            403, "Forbidden",
                            config.getErrorPages().get(403)
                        ).build();
                    }
                } else {
                    context.responseData = HttpResponse.errorResponse(
                        403, "Forbidden",
                        config.getErrorPages().get(403)
                    ).build();
                }
            } else if (java.nio.file.Files.exists(path)) {
                byte[] fileContent = java.nio.file.Files.readAllBytes(path);
                HttpResponse response = new HttpResponse();
                response.setBody(fileContent);
                response.setHeader("Content-Type", router.getMimeType(filePath));
                context.responseData = response.build();
            } else {
                context.responseData = HttpResponse.errorResponse(
                    404, "Not Found",
                    config.getErrorPages().get(404)
                ).build();
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            context.responseData = HttpResponse.errorResponse(
                500, "Internal Server Error",
                config.getErrorPages().get(500)
            ).build();
        }
    }
    
    private void handleCgiRequest(ClientContext context, ConfigLoader.Route route, HttpRequest request) {
     
        HttpResponse response = new HttpResponse();
        response.setStatus(501, "Not Implemented");
        response.setBody("CGI support not yet implemented");
        context.responseData = response.build();
    }
    
    private void cleanupTimeoutConnections() {
        long currentTime = System.currentTimeMillis();
        long timeout = config.getRequestTimeout();
        
        Iterator<Map.Entry<SocketChannel, ClientContext>> iter = 
            clientContexts.entrySet().iterator();
        
        while (iter.hasNext()) {
            Map.Entry<SocketChannel, ClientContext> entry = iter.next();
            ClientContext context = entry.getValue();
            
            if (currentTime - context.lastActivityTime > timeout) {
                try {
                    entry.getKey().close();
                } catch (IOException e) {
                }
                iter.remove();
                System.out.println("Connection timeout: " + entry.getKey());
            }
        }
    }
    
    private void closeClient(SocketChannel clientChannel, SelectionKey key) {
        try {
            key.cancel();
            clientChannel.close();
        } catch (IOException e) {
        }
        clientContexts.remove(clientChannel);
        System.out.println("Connection closed: " + clientChannel);
    }
    
    public void stop() {
        running = false;
        try {
            if (selector != null) {
                selector.wakeup(); // Wake up selector if it's waiting
                selector.close();
            }
        } catch (IOException e) {
            System.err.println("Error shutting down selector: " + e.getMessage());
        }
        
        // Shutdown CGI executor
        cgiExecutor.shutdown();
        try {
            if (!cgiExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cgiExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cgiExecutor.shutdownNow();
        }
        
        System.out.println("Server stopped.");
    }
    
 
    private class ClientContext {
        private ByteArrayOutputStream requestBuffer;  
        private byte[] responseData;                  
        private long startTime;                      
        private long lastActivityTime;               
        private boolean keepAlive;                   
        
        public ClientContext() {
            this.requestBuffer = new ByteArrayOutputStream();
            this.startTime = System.currentTimeMillis();
            this.lastActivityTime = this.startTime;
            this.keepAlive = false; 
        }
        
        public void appendRequestData(byte[] data) {
            requestBuffer.write(data, 0, data.length);
        }
        
        public boolean hasCompleteRequest() {
            byte[] data = requestBuffer.toByteArray();
            String requestStr = new String(data, java.nio.charset.StandardCharsets.UTF_8);
            
            int headerEnd = requestStr.indexOf("\r\n\r\n");
            if (headerEnd == -1) {
                return false; 
            }
            
            String headers = requestStr.substring(0, headerEnd);
            int contentLength = 0;
            
            for (String line : headers.split("\r\n")) {
                if (line.toLowerCase().startsWith("content-length:")) {
                    try {
                        contentLength = Integer.parseInt(line.substring(15).trim());
                    } catch (NumberFormatException e) {
                        return false; 
                    }
                    break;
                }
            }
            
            int bodyStart = headerEnd + 4; // Skip \r\n\r\n
            return data.length >= bodyStart + contentLength;
        }
        
        public HttpRequest getHttpRequest() {
            byte[] data = requestBuffer.toByteArray();
            
            String requestStr = new String(data, java.nio.charset.StandardCharsets.UTF_8);
            if (requestStr.contains("Transfer-Encoding: chunked")) {
                return HttpRequest.parseChunkedRequest(data);
            } else {
                return new HttpRequest(data);
            }
        }
        
        public void reset() {
            requestBuffer.reset();
            responseData = null;
            lastActivityTime = System.currentTimeMillis();
        }
    }
}