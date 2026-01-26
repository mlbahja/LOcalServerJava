package src;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class HttpResponse {
    private int statusCode;                
    private String statusMessage;          
    private Map<String, String> headers;   
    private byte[] body;                  
    // Removed: private List<Cookie> cookies; // We'll add this later
    
   
    public HttpResponse() {
        this.headers = new HashMap<>();
        // Removed: this.cookies = new ArrayList<>();
        this.statusCode = 200;
        this.statusMessage = "OK";
        setDefaultHeaders(); // Add standard headers
    }
    
    // Set standard HTTP headers
    private void setDefaultHeaders() {
        headers.put("Server", "Java-HTTP-Server/1.0");
        headers.put("Date", getCurrentDate()); // Current date in HTTP format
        headers.put("Connection", "close"); // Close connection after response
    }
    
    // Set HTTP status code and message
    public void setStatus(int code, String message) {
        this.statusCode = code;
        this.statusMessage = message;
    }
    
    // Add or modify a header
    public void setHeader(String key, String value) {
        headers.put(key, value);
    }
    
    // Set response body as byte array
    public void setBody(byte[] body) {
        this.body = body;
        // Automatically set Content-Length header
        headers.put("Content-Length", String.valueOf(body.length));
    }
    
    // Set response body as string (converts to bytes)
    public void setBody(String body) {
        setBody(body.getBytes(StandardCharsets.UTF_8));
        // Default to HTML content type for string bodies
        headers.put("Content-Type", "text/html; charset=utf-8");
    }
    
    // REMOVE THIS METHOD FOR NOW (Cookie support will be added by Member B)
    /*
    public void setCookie(Cookie cookie) {
        cookies.add(cookie);
    }
    */
    
    // Build the complete HTTP response as byte array
    public byte[] build() {
        StringBuilder response = new StringBuilder();
        
      
        response.append("HTTP/1.1 ")
                .append(statusCode)
                .append(" ")
                .append(statusMessage)
                .append("\r\n");
        
       
        for (Map.Entry<String, String> header : headers.entrySet()) {
            response.append(header.getKey())
                    .append(": ")
                    .append(header.getValue())
                    .append("\r\n");
        }
        
        // STEP 3: Cookies - REMOVED FOR NOW
        /*
        for (Cookie cookie : cookies) {
            response.append("Set-Cookie: ")
                    .append(cookie.toString())
                    .append("\r\n");
        }
        */
        
        response.append("\r\n");
        
        byte[] headerBytes = response.toString().getBytes(StandardCharsets.UTF_8);
        
        if (body != null && body.length > 0) {
            byte[] fullResponse = new byte[headerBytes.length + body.length];
            System.arraycopy(headerBytes, 0, fullResponse, 0, headerBytes.length);
            System.arraycopy(body, 0, fullResponse, headerBytes.length, body.length);
            return fullResponse;
        }
        
        return headerBytes;
    }
    
    public static HttpResponse errorResponse(int code, String message, String errorPagePath) {
        HttpResponse response = new HttpResponse();
        response.setStatus(code, message);
        
        try {
            if (errorPagePath != null) {
                java.nio.file.Path path = java.nio.file.Paths.get(errorPagePath);
                if (java.nio.file.Files.exists(path)) {
                    byte[] errorPage = java.nio.file.Files.readAllBytes(path);
                    response.setBody(errorPage);
                    response.setHeader("Content-Type", "text/html");
                } else {
                    response.setBody(createDefaultErrorPage(code, message));
                }
            } else {
                response.setBody(createDefaultErrorPage(code, message));
            }
        } catch (Exception e) {
            response.setBody(createDefaultErrorPage(code, message));
        }
        
        return response;
    }
    
    private static String createDefaultErrorPage(int code, String message) {
        return "<!DOCTYPE html>\n" +
               "<html>\n" +
               "<head>\n" +
               "    <title>" + code + " " + message + "</title>\n" +
               "</head>\n" +
               "<body>\n" +
               "    <h1>" + code + " " + message + "</h1>\n" +
               "    <p>The server encountered an error while processing your request.</p>\n" +
               "</body>\n" +
               "</html>";
    }
    
    private String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
            "EEE, dd MMM yyyy HH:mm:ss z", Locale.US
        );
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(new Date());
    }
    
    public int getStatusCode() { return statusCode; }
}