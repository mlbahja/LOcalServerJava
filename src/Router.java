
package src;

import java.io.File;
import java.util.*;

public class Router {
    private List<ConfigLoader.Route> routes;
    private Map<String, String> mimeTypes;
    
    public Router(List<ConfigLoader.Route> routes) {
        this.routes = routes;
        this.mimeTypes = new HashMap<>();
        initMimeTypes();
    }
    
    private void initMimeTypes() {
        mimeTypes.put("html", "text/html");
        mimeTypes.put("htm", "text/html");
        mimeTypes.put("css", "text/css");
        mimeTypes.put("js", "application/javascript");
        mimeTypes.put("json", "application/json");
        mimeTypes.put("png", "image/png");
        mimeTypes.put("jpg", "image/jpeg");
        mimeTypes.put("jpeg", "image/jpeg");
        mimeTypes.put("gif", "image/gif");
        mimeTypes.put("txt", "text/plain");
        mimeTypes.put("pdf", "application/pdf");
        mimeTypes.put("xml", "application/xml");
        mimeTypes.put("svg", "image/svg+xml");
    }
    
    public RouteMatch match(HttpRequest request) {
        String path = request.getPath();
        
        for (ConfigLoader.Route route : routes) { 
            if (path.startsWith(route.getPath())) {
                if (route.getRedirect() != null) {
                    return new RouteMatch(route, RouteMatch.Type.REDIRECT);
                }
                
                if (!route.getAllowedMethods().contains(request.getMethod())) {
                    return new RouteMatch(route, RouteMatch.Type.METHOD_NOT_ALLOWED);
                }
                
                if (route.getCgiExtension() != null && 
                    path.endsWith(route.getCgiExtension())) {
                    return new RouteMatch(route, RouteMatch.Type.CGI);
                }
                
                return new RouteMatch(route, RouteMatch.Type.STATIC);
            }
        }
        
        return null;
    }
    
    public String resolveFilePath(ConfigLoader.Route route, String requestPath) {
        String routePath = route.getPath();
        String relativePath = requestPath.substring(routePath.length());
        
        if (relativePath.isEmpty() || relativePath.equals("/")) {
            relativePath = "/" + (route.getDefaultFile() != null ? route.getDefaultFile() : "");
        }
        
        String fullPath = route.getRoot() + relativePath;
        
        File file = new File(fullPath);
        if (file.isDirectory()) {
            if (route.getDefaultFile() != null) {
                File indexFile = new File(fullPath, route.getDefaultFile());
                if (indexFile.exists()) {
                    return indexFile.getPath();
                }
            }
            
            if (route.isDirectoryListing()) {
                return fullPath;
            }
        }
        
        return fullPath;
    }
    
    public String getMimeType(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            String extension = filename.substring(dotIndex + 1).toLowerCase();
            return mimeTypes.getOrDefault(extension, "application/octet-stream");
        }
        return "application/octet-stream";
    }
    
    public byte[] generateDirectoryListing(String dirPath, String requestPath) {
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return null;
        }
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n")
            .append("<html>\n")
            .append("<head>\n")
            .append("<title>Index of ").append(requestPath).append("</title>\n")
            .append("</head>\n")
            .append("<body>\n")
            .append("<h1>Index of ").append(requestPath).append("</h1>\n")
            .append("<hr>\n")
            .append("<ul>\n");
        
        if (!requestPath.equals("/")) {
            html.append("<li><a href=\"../\">../</a></li>\n");
        }
        
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (file.isDirectory()) {
                    name += "/";
                }
                html.append("<li><a href=\"")
                    .append(name)
                    .append("\">")
                    .append(name)
                    .append("</a></li>\n");
            }
        }
        
        html.append("</ul>\n")
            .append("<hr>\n")
            .append("</body>\n")
            .append("</html>");
        
        return html.toString().getBytes();
    }
    
    public static class RouteMatch {
        public enum Type {
            STATIC,
            CGI,
            REDIRECT,
            METHOD_NOT_ALLOWED
        }
        
        private ConfigLoader.Route route;         
        private Type type;
        
        public RouteMatch(ConfigLoader.Route route, Type type) {
            this.route = route;
            this.type = type;
        }
        
        public ConfigLoader.Route getRoute() { return route; }
        public Type getType() { return type; }
    }
}