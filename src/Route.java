package src;

import java.util.*;

public class Route {
    private String path;               
    private List<String> allowedMethods; 
    private String root;              
    private String defaultFile;        
    private String cgiExtension;       
    private boolean directoryListing;  
    private String redirect;          
    
    public Route() {
        this.allowedMethods = new ArrayList<>();
        this.directoryListing = false;
    }
    
    
    public Route setPath(String path) {
        this.path = path;
        return this; 
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
    
   
    
    public String getPath() { return path; }
    public List<String> getAllowedMethods() { return allowedMethods; }
    public String getRoot() { return root; }
    public String getDefaultFile() { return defaultFile; }
    public String getCgiExtension() { return cgiExtension; }
    public boolean isDirectoryListing() { return directoryListing; }
    public String getRedirect() { return redirect; }
}