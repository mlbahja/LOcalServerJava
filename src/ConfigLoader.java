
package src;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public class ConfigLoader {
    public static class Config {
        private String host;
        private List<Integer> ports;
        private String defaultServer;
        private int clientBodySizeLimit;
        private int requestTimeout;
        private Map<Integer, String> errorPages;
        private List<Route> routes;
        
    
        public String getHost() { return host; }
        public List<Integer> getPorts() { 
            return ports != null ? Collections.unmodifiableList(ports) : Collections.emptyList(); 
        }
        public String getDefaultServer() { return defaultServer; }
        public int getClientBodySizeLimit() { return clientBodySizeLimit; }
        public int getRequestTimeout() { return requestTimeout; }
        public Map<Integer, String> getErrorPages() { 
            return errorPages != null ? Collections.unmodifiableMap(errorPages) : Collections.emptyMap(); 
        }
        public List<Route> getRoutes() { 
            return routes != null ? Collections.unmodifiableList(routes) : Collections.emptyList(); 
        }
        
        public void setHost(String host) { this.host = host; }
        public void setPorts(List<Integer> ports) { this.ports = ports; }
        public void setDefaultServer(String defaultServer) { this.defaultServer = defaultServer; }
        public void setClientBodySizeLimit(int clientBodySizeLimit) { this.clientBodySizeLimit = clientBodySizeLimit; }
        public void setRequestTimeout(int requestTimeout) { this.requestTimeout = requestTimeout; }
        public void setErrorPages(Map<Integer, String> errorPages) { this.errorPages = errorPages; }
        public void setRoutes(List<Route> routes) { this.routes = routes; }
    }
    
    public static class Route {
        private String path;
        private List<String> allowedMethods;
        private String root;
        private String defaultFile;
        private String cgiExtension;
        private boolean directoryListing;
        private String redirect;
        
        public String getPath() { return path; }
        public List<String> getAllowedMethods() { 
            return allowedMethods != null ? allowedMethods : new ArrayList<>(); 
        }
        public String getRoot() { return root; }
        public String getDefaultFile() { return defaultFile; }
        public String getCgiExtension() { return cgiExtension; }
        public boolean isDirectoryListing() { return directoryListing; }
        public String getRedirect() { return redirect; }
        
        public void setPath(String path) { this.path = path; }
        public void setAllowedMethods(List<String> allowedMethods) { this.allowedMethods = allowedMethods; }
        public void setRoot(String root) { this.root = root; }
        public void setDefaultFile(String defaultFile) { this.defaultFile = defaultFile; }
        public void setCgiExtension(String cgiExtension) { this.cgiExtension = cgiExtension; }
        public void setDirectoryListing(boolean directoryListing) { this.directoryListing = directoryListing; }
        public void setRedirect(String redirect) { this.redirect = redirect; }
    }
    
    public static Config load(String configPath) throws IOException {

        String jsonContent = readFileContent(configPath);
        //System.out.println("Parsing configuration file: " + configPath + "jsonContent ==> " + jsonContent);
        Map<String, Object> jsonMap = parseJsonManually(jsonContent);
        System.out.println("Parsed JSON Map: " + jsonMap);
        for (Object elem : jsonMap.entrySet()) {
            System.out.println("Element: " + elem.toString());
            
        }
        Config config = mapToConfig(jsonMap);
        
        applyDefaults(config);
        
        validateConfig(config);
        System.out.println("Configuration loaded successfully!");
        System.out.println("Host: " + config.getHost());
        System.out.println("Ports: " + config.getPorts());
        System.out.println("Routes: " + config.getRoutes().size());
        
        return config;
    }
    private static String readFileContent(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("Configuration file not found: " + filePath);
        }
        return Files.readString(path);
    }
    
    private static Map<String, Object> parseJsonManually(String json) throws IOException {
        Map<String, Object> result = new HashMap<>();
        json = json.trim();
        
        if (!json.startsWith("{") || !json.endsWith("}")) {
            throw new IOException("Invalid JSON: Expected object");
        }
        json = json.substring(1, json.length() - 1).trim();
        
        while (!json.isEmpty()) {
            int colonIndex = json.indexOf(':');
            if (colonIndex == -1) break;
            String key = json.substring(0, colonIndex).trim();
            if (!key.startsWith("\"") || !key.endsWith("\"")) {
                throw new IOException("Invalid JSON key: " + key);
            }
            key = key.substring(1, key.length() - 1);
            
            json = json.substring(colonIndex + 1).trim();
            Object value;
            int endIndex;
            
            if (json.startsWith("{")) {
                endIndex = findMatching(json, '{', '}');
                value = parseJsonManually(json.substring(0, endIndex));
            } else if (json.startsWith("[")) {
                endIndex = findMatching(json, '[', ']');
                value = parseJsonArray(json.substring(0, endIndex));
            } else if (json.startsWith("\"")) {
                endIndex = findStringEnd(json);
                value = json.substring(1, endIndex);
                endIndex++;
            } else if (json.startsWith("true")) {
                value = true;
                endIndex = 4;
            } else if (json.startsWith("false")) {
                value = false;
                endIndex = 5;
            } else if (json.startsWith("null")) {
                value = null;
                endIndex = 4;
            } else {
                endIndex = 0;
                while (endIndex < json.length() && 
                       (Character.isDigit(json.charAt(endIndex)) || 
                        json.charAt(endIndex) == '.' || 
                        json.charAt(endIndex) == '-')) {
                    endIndex++;
                }
                String numStr = json.substring(0, endIndex);
                try {
                    value = Integer.parseInt(numStr);
                } catch (NumberFormatException e1) {
                    try {
                        value = Double.parseDouble(numStr);
                    } catch (NumberFormatException e2) {
                        throw new IOException("Invalid number: " + numStr);
                    }
                }
            }
            result.put(key, value); 
            if (endIndex < json.length() && json.charAt(endIndex) == ',') {
                json = json.substring(endIndex + 1).trim();
            } else {
                json = json.substring(endIndex).trim();
            }
        }
        
        return result;
    }
    
    private static List<Object> parseJsonArray(String json) throws IOException {
        List<Object> result = new ArrayList<>();
        json = json.trim();
        
        if (!json.startsWith("[") || !json.endsWith("]")) {
            throw new IOException("Invalid JSON array");
        }
        
        json = json.substring(1, json.length() - 1).trim();
        
        while (!json.isEmpty()) {
            Object value;
            int endIndex;
            
            if (json.startsWith("{")) {
                endIndex = findMatching(json, '{', '}');
                value = parseJsonManually(json.substring(0, endIndex));
            } else if (json.startsWith("[")) {
                endIndex = findMatching(json, '[', ']');
                value = parseJsonArray(json.substring(0, endIndex));
            } else if (json.startsWith("\"")) {
                endIndex = findStringEnd(json);
                value = json.substring(1, endIndex);
                endIndex++; 
            } else if (json.startsWith("true")) {
                value = true;
                endIndex = 4;
            } else if (json.startsWith("false")) {
                value = false;
                endIndex = 5;
            } else if (json.startsWith("null")) {
                value = null;
                endIndex = 4;
            } else {
                endIndex = 0;
                while (endIndex < json.length() && 
                       (Character.isDigit(json.charAt(endIndex)) || 
                        json.charAt(endIndex) == '.' || 
                        json.charAt(endIndex) == '-')) {
                    endIndex++;
                }
                String numStr = json.substring(0, endIndex);
                try {
                    value = Integer.parseInt(numStr);
                } catch (NumberFormatException e1) {
                    try {
                        value = Double.parseDouble(numStr);
                    } catch (NumberFormatException e2) {
                        throw new IOException("Invalid number: " + numStr);
                    }
                }
            }
            
            result.add(value);
            if (endIndex < json.length() && json.charAt(endIndex) == ',') {
                json = json.substring(endIndex + 1).trim();
            } else {
                json = json.substring(endIndex).trim();
            }
        }
        
        return result;
    }
    
    private static int findMatching(String str, char open, char close) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == open) count++;
            else if (c == close) {
                count--;
                if (count == 0) return i + 1;
            }
        }
        return -1;
    }
    
    private static int findStringEnd(String str) {
        for (int i = 1; i < str.length(); i++) {
            if (str.charAt(i) == '"' && str.charAt(i - 1) != '\\') {
                return i;
            }
        }
        return -1;
    }
    
    private static Config mapToConfig(Map<String, Object> map) {
         Config config = new Config();
        
        if (map.containsKey("host")) {
            Object hostObj = map.get("host");
            config.setHost(hostObj != null ? hostObj.toString() : "localhost");
        }
        
        if (map.containsKey("ports")) {
            Object portsObj = map.get("ports");
            if (portsObj instanceof List) {
                List<Object> portsList = (List<Object>) portsObj;
                List<Integer> ports = new ArrayList<>();
                for (Object portObj : portsList) {
                    if (portObj instanceof Integer) {
                        ports.add((Integer) portObj);
                    } else if (portObj instanceof Double) {
                        ports.add(((Double) portObj).intValue());
                    } else if (portObj instanceof String) {
                        try {
                            ports.add(Integer.parseInt((String) portObj));
                        } catch (NumberFormatException e) {
                            ports.add(8080); // default
                        }
                    }
                }
                config.setPorts(ports);
            }
        }
        
        if (map.containsKey("defaultServer")) {
            Object serverObj = map.get("defaultServer");
            if (serverObj != null) {
                config.setDefaultServer(serverObj.toString());
            }
        }
        
        if (map.containsKey("clientBodySizeLimit")) {
            Object limitObj = map.get("clientBodySizeLimit");
            if (limitObj instanceof Integer) {
                config.setClientBodySizeLimit((Integer) limitObj);
            } else if (limitObj instanceof Double) {
                config.setClientBodySizeLimit(((Double) limitObj).intValue());
            } else if (limitObj instanceof String) {
                try {
                    config.setClientBodySizeLimit(Integer.parseInt((String) limitObj));
                } catch (NumberFormatException e) {
                    config.setClientBodySizeLimit(10 * 1024 * 1024);
                }
            }
        }
        
        if (map.containsKey("requestTimeout")) {
            Object timeoutObj = map.get("requestTimeout");
            if (timeoutObj instanceof Integer) {
                config.setRequestTimeout((Integer) timeoutObj);
            } else if (timeoutObj instanceof Double) {
                config.setRequestTimeout(((Double) timeoutObj).intValue());
            } else if (timeoutObj instanceof String) {
                try {
                    config.setRequestTimeout(Integer.parseInt((String) timeoutObj));
                } catch (NumberFormatException e) {
                    config.setRequestTimeout(30000);
                }
            }
        }
        
        if (map.containsKey("errorPages")) {
            Object errorPagesObj = map.get("errorPages");
            if (errorPagesObj instanceof Map) {
                Map<?, ?> errorPagesMap = (Map<?, ?>) errorPagesObj;
                Map<Integer, String> errorPages = new HashMap<>();
                for (Map.Entry<?, ?> entry : errorPagesMap.entrySet()) {
                    try {
                        int code = Integer.parseInt(entry.getKey().toString());
                        String path = entry.getValue() != null ? entry.getValue().toString() : "";
                        errorPages.put(code, path);
                    } catch (NumberFormatException e) {
                        
                    }
                }
                config.setErrorPages(errorPages);
            }
        }
        
        if (map.containsKey("routes")) {
            Object routesObj = map.get("routes");
            if (routesObj instanceof List) {
                List<Object> routesList = (List<Object>) routesObj;
                List<Route> routes = new ArrayList<>();
                for (Object routeObj : routesList) {
                    if (routeObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> routeMap = (Map<String, Object>) routeObj;
                        routes.add(mapToRoute(routeMap));
                    }
                }
                config.setRoutes(routes);
            }
        }
        
        return config;
    }
    
    private static Route mapToRoute(Map<String, Object> map) {
        Route route = new Route();
        
        if (map.containsKey("path")) {
            Object pathObj = map.get("path");
            route.setPath(pathObj != null ? pathObj.toString() : "/");
        }
        
        if (map.containsKey("allowedMethods")) {
            Object methodsObj = map.get("allowedMethods");
            if (methodsObj instanceof List) {
                List<Object> methodsList = (List<Object>) methodsObj;
                List<String> methods = new ArrayList<>();
                for (Object methodObj : methodsList) {
                    if (methodObj != null) {
                        methods.add(methodObj.toString().toUpperCase());
                    }
                }
                route.setAllowedMethods(methods);
            }
        }
        
        if (map.containsKey("root")) {
            Object rootObj = map.get("root");
            if (rootObj != null) {
                route.setRoot(rootObj.toString());
            }
        }
        
        if (map.containsKey("defaultFile")) {
            Object fileObj = map.get("defaultFile");
            if (fileObj != null) {
                route.setDefaultFile(fileObj.toString());
            }
        }
        
        if (map.containsKey("cgiExtension")) {
            Object cgiObj = map.get("cgiExtension");
            if (cgiObj != null) {
                route.setCgiExtension(cgiObj.toString());
            }
        }
        
        if (map.containsKey("directoryListing")) {
            Object listingObj = map.get("directoryListing");
            if (listingObj instanceof Boolean) {
                route.setDirectoryListing((Boolean) listingObj);
            } else if (listingObj instanceof String) {
                route.setDirectoryListing(Boolean.parseBoolean((String) listingObj));
            }
        }
        
        if (map.containsKey("redirect")) {
            Object redirectObj = map.get("redirect");
            if (redirectObj != null) {
                route.setRedirect(redirectObj.toString());
            }
        }
        
        return route;
    }
    
    private static void applyDefaults(Config config) {
        if (config.getHost() == null || config.getHost().trim().isEmpty()) {
            config.setHost("localhost");
        }
        if (config.getPorts() == null || config.getPorts().isEmpty()) {
            config.setPorts(Arrays.asList(8080));
        }
        if (config.getClientBodySizeLimit() <= 0) {
            config.setClientBodySizeLimit(10 * 1024 * 1024);
        }
        if (config.getRequestTimeout() <= 0) {
            config.setRequestTimeout(30000);
        }
        
        if (config.getErrorPages() == null || config.getErrorPages().isEmpty()) {
            Map<Integer, String> errorPages = new HashMap<>();
            errorPages.put(400, "error_pages/400.html");
            errorPages.put(403, "error_pages/403.html");
            errorPages.put(404, "error_pages/404.html");
            errorPages.put(405, "error_pages/405.html");
            errorPages.put(413, "error_pages/413.html");
            errorPages.put(500, "error_pages/500.html");
            config.setErrorPages(errorPages);
        }
        
        if (config.getRoutes() == null) {
            config.setRoutes(new ArrayList<>());
        }
        
        for (Route route : config.getRoutes()) {
            if (route.getPath() == null || route.getPath().trim().isEmpty()) {
                route.setPath("/");
            }
            
            if (!route.getPath().startsWith("/")) {
                route.setPath("/" + route.getPath());
            }
            
            if (route.getAllowedMethods() == null || route.getAllowedMethods().isEmpty()) {
                route.setAllowedMethods(Arrays.asList("GET"));
            }
            
            List<String> upperMethods = new ArrayList<>();
            for (String method : route.getAllowedMethods()) {
                if (method != null && !method.trim().isEmpty()) {
                    upperMethods.add(method.toUpperCase());
                }
            }
            if (upperMethods.isEmpty()) {
                upperMethods.add("GET");
            }
            route.setAllowedMethods(upperMethods);
            
            if (route.getRedirect() == null && 
                (route.getRoot() == null || route.getRoot().trim().isEmpty())) {
                route.setRoot("public");
            }
            
            if (route.getDefaultFile() == null) {
                route.setDefaultFile("index.html");
            }
            
            if (route.getCgiExtension() != null && !route.getCgiExtension().trim().isEmpty()) {
                if (!route.getCgiExtension().startsWith(".")) {
                    route.setCgiExtension("." + route.getCgiExtension());
                }
            }
        }
        
        if (config.getRoutes().isEmpty()) {
            Route defaultRoute = new Route();
            defaultRoute.setPath("/");
            defaultRoute.setAllowedMethods(Arrays.asList("GET"));
            defaultRoute.setRoot("public");
            defaultRoute.setDefaultFile("index.html");
            defaultRoute.setDirectoryListing(false);
            config.getRoutes().add(defaultRoute);
        }
    }
    
    private static void validateConfig(Config config) throws IOException {
        if (config == null) {
            throw new IOException("Configuration is null");
        }
        
        for (Integer port : config.getPorts()) {
            if (port <= 0 || port > 65535) {
                throw new IOException("Invalid port: " + port);
            }
        }
        
        for (Route route : config.getRoutes()) {
            if (route.getAllowedMethods() == null || route.getAllowedMethods().isEmpty()) {
                throw new IOException("Route " + route.getPath() + " must have allowed methods");
            }
            
            if (route.getRedirect() == null && 
                (route.getRoot() == null || route.getRoot().trim().isEmpty())) {
                throw new IOException("Route " + route.getPath() + " must have root directory");
            }
        }
    }

}