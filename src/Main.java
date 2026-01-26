package src;

import java.io.IOException;

public class Main {
    // Entry point of the HTTP server
    public static void main(String[] args) {
        try {
            System.out.println("Starting HTTP Server...");
            
            // STEP 1: Create configuration programmatically
            Config config = createConfig();
            
            // STEP 2: Create server instance with configuration
            Server server = new Server(config);
            
            // STEP 3: Start the server (this will block)
            server.start();
            
            // STEP 4: Setup graceful shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down server...");
                server.stop();
            }));
            
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    // Create server configuration (instead of loading from JSON)
    private static Config createConfig() {
        // Build configuration using fluent interface
        Config config = new Config()
            .setHost("localhost")          // Bind to localhost
            .addPort(8080)                 // Listen on port 8080
            .addPort(8081)                 // Also listen on port 8081 (optional)
            .setDefaultServer("localhost:8080") // Default virtual host
            .setClientBodySizeLimit(10 * 1024 * 1024) // 10MB upload limit
            .setRequestTimeout(30000)      // 30 second request timeout
            // Configure error pages
            .addErrorPage(400, "error_pages/400.html")
            .addErrorPage(403, "error_pages/403.html")
            .addErrorPage(404, "error_pages/404.html")
            .addErrorPage(405, "error_pages/405.html")
            .addErrorPage(413, "error_pages/413.html")
            .addErrorPage(500, "error_pages/500.html");
        
        // Define routes for the server
        
        // Route 1: Serve static files from /public directory
        Route homeRoute = new Route()
            .setPath("/")                   // Match root path
            .addAllowedMethod("GET")        // Only allow GET requests
            .setRoot("public")              // Files are in "public" directory
            .setDefaultFile("index.html")   // Default file for directory
            .setDirectoryListing(false);    // Don't show directory listing
        
        // Route 2: API endpoint
        Route apiRoute = new Route()
            .setPath("/api")
            .addAllowedMethod("GET")
            .addAllowedMethod("POST")       // Allow both GET and POST
            .setRoot("api")                 // API files directory
            .setDefaultFile("index.json")
            .setDirectoryListing(false);
        
        // Route 3: File uploads directory
        Route uploadRoute = new Route()
            .setPath("/uploads")
            .addAllowedMethod("GET")
            .addAllowedMethod("POST")
            .addAllowedMethod("DELETE")     // Allow file deletion
            .setRoot("uploads")             // Upload storage directory
            .setDirectoryListing(true);     // Show directory listing
        
        // Route 4: CGI scripts (Python)
        Route cgiRoute = new Route()
            .setPath("/cgi-bin")
            .addAllowedMethod("GET")
            .addAllowedMethod("POST")
            .setRoot("cgi-bin")             // CGI scripts directory
            .setCgiExtension(".py")         // Execute .py files as CGI
            .setDirectoryListing(false);
        
        // Route 5: Redirect example
        Route redirectRoute = new Route()
            .setPath("/old")
            .addAllowedMethod("GET")
            .setRedirect("/new");           // Permanent redirect
        
        // Add all routes to configuration
        config.addRoute(homeRoute)
              .addRoute(apiRoute)
              .addRoute(uploadRoute)
              .addRoute(cgiRoute)
              .addRoute(redirectRoute);
        
        return config;
    }
}