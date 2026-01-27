package src;

import java.io.IOException;
public class Main {
    public static void main(String[] args) {
        try {
            ConfigLoader.Config config = ConfigLoader.load("config.json");
            Server server = new Server(config);
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down server...");
                server.stop();
            }));
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            System.exit(1);
        }
    }
}
