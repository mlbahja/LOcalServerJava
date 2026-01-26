package src;

import java.io.File;
import java.nio.file.*;
import java.util.*;

public class Router {
    private List<Route> routes;       // All configured routes
    private Map<String, String> mimeTypes; // File extension to MIME type mapping
    
    public Router(List<Route> routes) {
        this.routes = routes;
        this.mimeTypes = new HashMap<>();
        initMimeTypes(); // Initialize MIME type database
    }
    
    // Initialize common MIME types
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
    
    // Find matching route for a request
    public RouteMatch match(HttpRequest request) {
        String path = request.getPath();
        
        // Check each route for a match
        for (Route route : routes) {
            if (path.startsWith(route.getPath())) {
                // Check for redirect first
                if (route.getRedirect() != null) {
                    return new RouteMatch(route, RouteMatch.Type.REDIRECT);
                }
                
                // Check if HTTP method is allowed
                if (!route.getAllowedMethods().contains(request.getMethod())) {
                    return new RouteMatch(route, RouteMatch.Type.METHOD_NOT_ALLOWED);
                }
                
                // Check if this is a CGI script
                if (route.getCgiExtension() != null && 
                    path.endsWith(route.getCgiExtension())) {
                    return new RouteMatch(route, RouteMatch.Type.CGI);
                }
                
                // Default: static file serving
                return new RouteMatch(route, RouteMatch.Type.STATIC);
            }
        }
        
        return null; // No route found (will result in 404)
    }
    
    // Convert request path to filesystem path
    public String resolveFilePath(Route route, String requestPath) {
        String routePath = route.getPath();
        String relativePath = requestPath.substring(routePath.length());
        
        // Handle root path (empty or "/")
        if (relativePath.isEmpty() || relativePath.equals("/")) {
            relativePath = "/" + (route.getDefaultFile() != null ? route.getDefaultFile() : "");
        }
        
        // Combine route root with relative path
        String fullPath = route.getRoot() + relativePath;
        
        // Check if path is a directory
        File file = new File(fullPath);
        if (file.isDirectory()) {
            // Try to serve default file
            if (route.getDefaultFile() != null) {
                File indexFile = new File(fullPath, route.getDefaultFile());
                if (indexFile.exists()) {
                    return indexFile.getPath();
                }
            }
            
            // If directory listing is enabled, return directory path
            if (route.isDirectoryListing()) {
                return fullPath; // Special case for directory listing
            }
        }
        
        return fullPath;
    }
    
    // Determine MIME type from file extension
    public String getMimeType(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            String extension = filename.substring(dotIndex + 1).toLowerCase();
            return mimeTypes.getOrDefault(extension, "application/octet-stream");
        }
        return "application/octet-stream"; // Default binary type
    }
    
    // Generate HTML directory listing
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
        
        // Add parent directory link (if not at root)
        if (!requestPath.equals("/")) {
            html.append("<li><a href=\"../\">../</a></li>\n");
        }
        
        // List all files and directories
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (file.isDirectory()) {
                    name += "/"; // Add slash for directories
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
    
    // Inner class to represent route matching result
    public static class RouteMatch {
        public enum Type {
            STATIC,           // Serve static file
            CGI,              // Execute CGI script
            REDIRECT,         // HTTP redirect
            METHOD_NOT_ALLOWED // 405 Method Not Allowed
        }
        
        private Route route;   // Matched route
        private Type type;     // Type of match
        
        public RouteMatch(Route route, Type type) {
            this.route = route;
            this.type = type;
        }
        
        public Route getRoute() { return route; }
        public Type getType() { return type; }
    }
}