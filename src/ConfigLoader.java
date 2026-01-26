package src; 

public class ConfigLoader {
    public static Config load() {
        Config config = new Config();
        
       
        config.setHost("127.0.0.1")
              .setClientBodySizeLimit(1000000)
              .setDefaultServer("127.0.0.1:8080")
              .addPort(8080)
              .addErrorPage(404, "error_pages/404.html")
              .addErrorPage(500, "error_pages/500.html");
        
        Route rootRoute = new Route()
            .setPath("/")
            .addAllowedMethod("GET")
            .setRoot("www")
            .setDefaultFile("index.html")
            .setDirectoryListing(false);
        
        config.addRoute(rootRoute);
        
        return config;
    }
}