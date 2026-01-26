package src;

import java.util.*;

public class Config {
    private String host;
    private List<Integer> ports;
    private String defaultServer;
    private int clientBodySizeLimit;
    private int requestTimeout;
    private Map<Integer, String> errorPages;
    private List<Route> routes;
    
    public Config() {
        this.host = "localhost";
        this.ports = new ArrayList<>();
        this.errorPages = new HashMap<>();
        this.routes = new ArrayList<>();
        this.clientBodySizeLimit = 10 * 1024 * 1024; 
        this.requestTimeout = 30000; 
    }
    
    public Config setHost(String host) {
        this.host = host;
        return this; 
    }
    
    public Config addPort(int port) {
        this.ports.add(port);
        return this;
    }
    
    public Config setDefaultServer(String defaultServer) {
        this.defaultServer = defaultServer;
        return this;
    }
    
    public Config setClientBodySizeLimit(int limit) {
        this.clientBodySizeLimit = limit;
        return this;
    }
    
    public Config setRequestTimeout(int timeout) {
        this.requestTimeout = timeout;
        return this;
    }
    
    public Config addErrorPage(int code, String path) {
        this.errorPages.put(code, path);
        return this;
    }
    
    public Config addRoute(Route route) {
        this.routes.add(route);
        return this;
    }
    
    
    public String getHost() { return host; }
    public List<Integer> getPorts() { return ports; }
    public String getDefaultServer() { return defaultServer; }
    public int getClientBodySizeLimit() { return clientBodySizeLimit; }
    public int getRequestTimeout() { return requestTimeout; }
    public Map<Integer, String> getErrorPages() { return errorPages; }
    public List<Route> getRoutes() { return routes; }
}