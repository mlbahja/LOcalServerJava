package src;

import java.util.*;

public class Route {
    private String path;               // Route path (e.g., "/", "/api")
    private List<String> allowedMethods; // GET, POST, DELETE
    private String root;               // Directory root for files
    private String defaultFile;        // Default file (index.html)
    private String cgiExtension;       // File extension for CGI (.py)
    private boolean directoryListing;  // Allow directory listing
    private String redirect;           // Redirect URL
    
    public Route() {
        this.allowedMethods = new ArrayList<>();
        this.directoryListing = false;
    }
    
    // Builder-style methods for easy configuration
    
    public Route setPath(String path) {
        this.path = path;
        return this; // Enable method chaining
    }
    
    public Route addAllowedMethod(String method) {
        this.allowedMethods.add(method);
        return this;
    }
    
    public Route setRoot(String root) {
        this.root = root;
        return this;
    }
    
    public Route setDefaultFile(String defaultFile) {
        this.defaultFile = defaultFile;
        return this;
    }
    
    public Route setCgiExtension(String cgiExtension) {
        this.cgiExtension = cgiExtension;
        return this;
    }
    
    public Route setDirectoryListing(boolean directoryListing) {
        this.directoryListing = directoryListing;
        return this;
    }
    
    public Route setRedirect(String redirect) {
        this.redirect = redirect;
        return this;
    }
    
    // Getters - used by Router and Server
    
    public String getPath() { return path; }
    public List<String> getAllowedMethods() { return allowedMethods; }
    public String getRoot() { return root; }
    public String getDefaultFile() { return defaultFile; }
    public String getCgiExtension() { return cgiExtension; }
    public boolean isDirectoryListing() { return directoryListing; }
    public String getRedirect() { return redirect; }
}