package src;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private Config config;  // Server configuration
    private Selector selector;  // NIO selector for event-driven I/O
    private Map<SocketChannel, ClientContext> clientContexts; // Track client state
    private Router router;  // Request router
    private ExecutorService cgiExecutor;  // Thread pool for CGI execution
    private volatile boolean running;  // Server running flag
    
    public Server(Config config) {
        this.config = config;
        this.clientContexts = new ConcurrentHashMap<>();
        this.router = new Router(config.getRoutes());
        this.cgiExecutor = Executors.newCachedThreadPool();
    }
    
    // Start the server
    public void start() throws IOException {
        selector = Selector.open();
        running = true;
        
        System.out.println("Starting HTTP Server...");
        
        // STEP 1: Open server sockets for all configured ports
        for (int port : config.getPorts()) {
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);  // Non-blocking mode
            serverChannel.socket().bind(new InetSocketAddress(config.getHost(), port));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("  Listening on " + config.getHost() + ":" + port);
        }
        
        System.out.println("Server ready. Press Ctrl+C to stop.");
        
        // STEP 2: Main event loop
        while (running) {
            try {
                // Wait for events with timeout
                selector.select(config.getRequestTimeout());
                
                // Process all ready events
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();
                
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();
                    
                    if (!key.isValid()) {
                        continue;
                    }
                    
                    // Handle different event types
                    if (key.isAcceptable()) {
                        acceptConnection(key);
                    } else if (key.isReadable()) {
                        readData(key);
                    } else if (key.isWritable()) {
                        writeData(key);
                    }
                }
                
                // STEP 3: Cleanup timed out connections
                cleanupTimeoutConnections();
                
            } catch (IOException e) {
                System.err.println("Error in selector loop: " + e.getMessage());
                e.printStackTrace();
            } catch (ClosedSelectorException e) {
                // Selector closed, normal shutdown
                break;
            }
        }
    }
    
    // Handle new client connection
    private void acceptConnection(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        
        // Create context to track this client's state
        ClientContext context = new ClientContext();
        context.startTime = System.currentTimeMillis();
        context.lastActivityTime = context.startTime;
        clientContexts.put(clientChannel, context);
        
        // Register for read operations
        clientChannel.register(selector, SelectionKey.OP_READ, context);
        
        System.out.println("New connection from: " + clientChannel.getRemoteAddress());
    }
    
    // Read data from client
    private void readData(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ClientContext context = (ClientContext) key.attachment();
        
        ByteBuffer buffer = ByteBuffer.allocate(8192); // 8KB buffer
        int bytesRead;
        
        try {
            bytesRead = clientChannel.read(buffer);
        } catch (IOException e) {
            // Client disconnected unexpectedly
            closeClient(clientChannel, key);
            return;
        }
        
        if (bytesRead == -1) {
            // Client disconnected gracefully
            closeClient(clientChannel, key);
            return;
        }
        
        if (bytesRead > 0) {
            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            
            // Append to request buffer
            context.appendRequestData(data);
            context.lastActivityTime = System.currentTimeMillis();
            
            // Check if we have a complete HTTP request
            if (context.hasCompleteRequest()) {
                // Process the request
                processRequest(clientChannel, context);
                // Switch to write mode to send response
                key.interestOps(SelectionKey.OP_WRITE);
            }
        }
    }
    
    // Write data to client
    private void writeData(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ClientContext context = (ClientContext) key.attachment();
        
        if (context.responseData != null) {
            ByteBuffer buffer = ByteBuffer.wrap(context.responseData);
            
            try {
                int bytesWritten = clientChannel.write(buffer);
                
                if (bytesWritten > 0 && !buffer.hasRemaining()) {
                    // Response fully sent
                    if (context.keepAlive) {
                        // Reset for next request (keep-alive)
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
    
    // Process a complete HTTP request
    private void processRequest(SocketChannel clientChannel, ClientContext context) {
        try {
            // Parse the HTTP request
            HttpRequest request = context.getHttpRequest();
            
            // Check if body exceeds size limit
            if (request.getBody().length > config.getClientBodySizeLimit()) {
                context.responseData = HttpResponse.errorResponse(
                    413, "Payload Too Large",
                    config.getErrorPages().get(413)
                ).build();
                return;
            }
            
            // Find matching route
            Router.RouteMatch match = router.match(request);
            
            if (match == null) {
                // No route found - 404 Not Found
                context.responseData = HttpResponse.errorResponse(
                    404, "Not Found",
                    config.getErrorPages().get(404)
                ).build();
                return;
            }
            
            // Handle based on route type
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
    
    // Handle HTTP redirect
    private void handleRedirect(ClientContext context, Route route) {
        HttpResponse response = new HttpResponse();
        response.setStatus(301, "Moved Permanently");
        response.setHeader("Location", route.getRedirect());
        response.setBody("");
        context.responseData = response.build();
    }
    
    // Serve static file
    private void handleStaticFile(ClientContext context, Route route, HttpRequest request) {
        String filePath = router.resolveFilePath(route, request.getPath());
        
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(filePath);
            
            if (java.nio.file.Files.isDirectory(path)) {
                // Directory requested
                if (route.isDirectoryListing()) {
                    // Generate directory listing
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
                    // Directory listing not allowed
                    context.responseData = HttpResponse.errorResponse(
                        403, "Forbidden",
                        config.getErrorPages().get(403)
                    ).build();
                }
            } else if (java.nio.file.Files.exists(path)) {
                // File exists - serve it
                byte[] fileContent = java.nio.file.Files.readAllBytes(path);
                HttpResponse response = new HttpResponse();
                response.setBody(fileContent);
                response.setHeader("Content-Type", router.getMimeType(filePath));
                context.responseData = response.build();
            } else {
                // File not found
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
    
    // Handle CGI request (stub - Member B will implement)
    private void handleCgiRequest(ClientContext context, Route route, HttpRequest request) {
        // This will be implemented by Member B
        // For now, return 501 Not Implemented
        HttpResponse response = new HttpResponse();
        response.setStatus(501, "Not Implemented");
        response.setBody("CGI support not yet implemented");
        context.responseData = response.build();
    }
    
    // Clean up connections that have timed out
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
                    // Ignore during cleanup
                }
                iter.remove();
                System.out.println("Connection timeout: " + entry.getKey());
            }
        }
    }
    
    // Close client connection
    private void closeClient(SocketChannel clientChannel, SelectionKey key) {
        try {
            key.cancel();
            clientChannel.close();
        } catch (IOException e) {
            // Ignore during close
        }
        clientContexts.remove(clientChannel);
        System.out.println("Connection closed: " + clientChannel);
    }
    
    // Stop the server
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
    
    // Inner class to track client state
    private class ClientContext {
        private ByteArrayOutputStream requestBuffer;  // Accumulated request data
        private byte[] responseData;                  // Response to send
        private long startTime;                       // Connection start time
        private long lastActivityTime;                // Last I/O activity
        private boolean keepAlive;                    // HTTP keep-alive flag
        
        public ClientContext() {
            this.requestBuffer = new ByteArrayOutputStream();
            this.startTime = System.currentTimeMillis();
            this.lastActivityTime = this.startTime;
            this.keepAlive = false; // Default to close connection
        }
        
        // Append incoming data to request buffer
        public void appendRequestData(byte[] data) {
            requestBuffer.write(data, 0, data.length);
        }
        
        // Check if we have a complete HTTP request
        public boolean hasCompleteRequest() {
            byte[] data = requestBuffer.toByteArray();
            String requestStr = new String(data, java.nio.charset.StandardCharsets.UTF_8);
            
            // Find end of headers
            int headerEnd = requestStr.indexOf("\r\n\r\n");
            if (headerEnd == -1) {
                return false; // Headers not complete
            }
            
            // Check Content-Length header
            String headers = requestStr.substring(0, headerEnd);
            int contentLength = 0;
            
            // Look for Content-Length header
            for (String line : headers.split("\r\n")) {
                if (line.toLowerCase().startsWith("content-length:")) {
                    try {
                        contentLength = Integer.parseInt(line.substring(15).trim());
                    } catch (NumberFormatException e) {
                        return false; // Invalid Content-Length
                    }
                    break;
                }
            }
            
            // Check if we have received the complete body
            int bodyStart = headerEnd + 4; // Skip \r\n\r\n
            return data.length >= bodyStart + contentLength;
        }
        
        // Parse the complete request
        public HttpRequest getHttpRequest() {
            byte[] data = requestBuffer.toByteArray();
            
            // Check for chunked encoding
            String requestStr = new String(data, java.nio.charset.StandardCharsets.UTF_8);
            if (requestStr.contains("Transfer-Encoding: chunked")) {
                return HttpRequest.parseChunkedRequest(data);
            } else {
                return new HttpRequest(data);
            }
        }
        
        // Reset for next request (keep-alive)
        public void reset() {
            requestBuffer.reset();
            responseData = null;
            lastActivityTime = System.currentTimeMillis();
        }
    }
}