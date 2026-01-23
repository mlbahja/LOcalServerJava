public class Main {

    public static void main(String[] args) {
        System.out.println("Starting Java HTTP Server...");

        try {
            Config config = ConfigLoader.load();

            System.out.println("Host: " + config.host);
            System.out.println("Ports: " + config.ports);
            System.out.println("Routes count: " + config.routes.size());

        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
