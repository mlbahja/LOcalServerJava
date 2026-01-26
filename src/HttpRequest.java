package src;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpRequest {
    private String method;  
    private String path;         
    private String version;      
    private Map<String, String> headers;  
    private Map<String, String> cookies;  
    private Map<String, String> queryParams; 
    private byte[] body;         
    private boolean isChunked;    // Chunked transfer encoding
    
    // Constructor: parse raw HTTP request bytes
    public HttpRequest(byte[] rawRequest) {
        this.headers = new HashMap<>();
        this.cookies = new HashMap<>();
        this.queryParams = new HashMap<>();
        parseRequest(rawRequest);
    }
    
    // Main parsing method
    private void parseRequest(byte[] rawRequest) {
        String requestStr = new String(rawRequest, StandardCharsets.UTF_8);
        String[] lines = requestStr.split("\r\n");
        
        if (lines.length == 0) return;
        
        String[] requestLine = lines[0].split(" ");
        if (requestLine.length >= 3) {
            this.method = requestLine[0];
            parsePathAndQuery(requestLine[1]); // Extract path and query params
            this.version = requestLine[2];
        }
        
        int i = 1;
        for (; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) break; 
            
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                headers.put(key, value);
                
                if (key.equalsIgnoreCase("Transfer-Encoding") && 
                    value.equalsIgnoreCase("chunked")) {
                    this.isChunked = true;
                }
                
                if (key.equalsIgnoreCase("Cookie")) {
                    parseCookies(value);
                }
            }
        }
        
        if (i < lines.length - 1) {
            StringBuilder bodyBuilder = new StringBuilder();
            for (int j = i + 1; j < lines.length; j++) {
                bodyBuilder.append(lines[j]);
                if (j < lines.length - 1) {
                    bodyBuilder.append("\r\n");
                }
            }
            this.body = bodyBuilder.toString().getBytes(StandardCharsets.UTF_8);
        } else {
            this.body = new byte[0];
        }
    }
    
    private void parsePathAndQuery(String fullPath) {
        int queryIndex = fullPath.indexOf('?');
        if (queryIndex > 0) {
            this.path = fullPath.substring(0, queryIndex);
            String query = fullPath.substring(queryIndex + 1);
            parseQueryString(query);
        } else {
            this.path = fullPath;
        }
    }
    
    private void parseQueryString(String query) {
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                queryParams.put(kv[0], kv[1]);
            }
        }
    }
    
    private void parseCookies(String cookieHeader) {
        String[] cookies = cookieHeader.split(";");
        for (String cookie : cookies) {
            String[] parts = cookie.split("=");
            if (parts.length == 2) {
                this.cookies.put(parts[0].trim(), parts[1].trim());
            }
        }
    }
    
    public static HttpRequest parseChunkedRequest(byte[] rawRequest) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        String requestStr = new String(rawRequest, StandardCharsets.UTF_8);
        String[] lines = requestStr.split("\r\n");
        
     
        int i = 0;
        for (; i < lines.length; i++) {
            if (lines[i].isEmpty()) {
                i++;
                break;
            }
        }
        
   
        while (i < lines.length) {
            try {
                String chunkSizeStr = lines[i];
                int chunkSize = Integer.parseInt(chunkSizeStr, 16);
                
                if (chunkSize == 0) {
                    break; // Last chunk reached
                }
                
                i++; 
                if (i < lines.length) {
                    
                    try {
                        body.write(lines[i].getBytes(StandardCharsets.UTF_8));
                    } catch (java.io.IOException e) {
                       
                        break;
                    }
                    i += 2; 
                }
            } catch (NumberFormatException e) {
                break;
            }
        }
        
        
        HttpRequest request = new HttpRequest(rawRequest);
        request.body = body.toByteArray();
        request.isChunked = true;
        return request;
    }
    
  
    
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public String getVersion() { return version; }
    public Map<String, String> getHeaders() { return headers; }
    public String getHeader(String key) { return headers.get(key); }
    public byte[] getBody() { return body; }
    public Map<String, String> getCookies() { return cookies; }
    public Map<String, String> getQueryParams() { return queryParams; }
    public boolean isChunked() { return isChunked; }
}