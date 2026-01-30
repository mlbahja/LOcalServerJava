package src;

import java.util.List;
import java.util.Map;

public class Config {
    private String host;
    private List<Integer> ports;
    private String defaultServer;
    private int clientBodySizeLimit;
    private int requestTimeout;
    private Map<Integer, String> errorPages;
    private List<Route> routes;
    
    public static class Route {
        private String path;
        private List<String> allowedMethods;
        private String root;
        private String defaultFile;
        private String cgiExtension;
        private boolean directoryListing;
        private String redirect;
        
        public String getPath() { return path; }
        public List<String> getAllowedMethods() { return allowedMethods; }
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
    
    public String getHost() { return host; }
    public List<Integer> getPorts() { return ports; }
    public String getDefaultServer() { return defaultServer; }
    public int getClientBodySizeLimit() { return clientBodySizeLimit; }
    public int getRequestTimeout() { return requestTimeout; }
    public Map<Integer, String> getErrorPages() { return errorPages; }
    public List<Route> getRoutes() { return routes; }
    
    public void setHost(String host) { this.host = host; }
    public void setPorts(List<Integer> ports) { this.ports = ports; }
    public void setDefaultServer(String defaultServer) { this.defaultServer = defaultServer; }
    public void setClientBodySizeLimit(int clientBodySizeLimit) { this.clientBodySizeLimit = clientBodySizeLimit; }
    public void setRequestTimeout(int requestTimeout) { this.requestTimeout = requestTimeout; }
    public void setErrorPages(Map<Integer, String> errorPages) { this.errorPages = errorPages; }
    public void setRoutes(List<Route> routes) { this.routes = routes; }
}