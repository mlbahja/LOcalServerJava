import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConfigLoader {

    public static Config load() {
        Config config = new Config();

        // server level config
        config.host = "127.0.0.1";
        config.clientBodyLimit = 1000000;
        config.defaultServer = true;

        // ports
        config.ports = new ArrayList<>();
        config.ports.add(8080);

        // error pages
        config.errorPages = new HashMap<>();
        config.errorPages.put(404, "error_pages/404.html");
        config.errorPages.put(500, "error_pages/500.html");

        // routes
        List<Route> routes = new ArrayList<>();

        Route rootRoute = new Route();
        rootRoute.path = "/";
        rootRoute.methods = List.of("GET");
        rootRoute.root = "www";
        rootRoute.defaultFile = "index.html";
        rootRoute.directoryListing = false;

        routes.add(rootRoute);

        config.routes = routes;

        return config;
    }
}
