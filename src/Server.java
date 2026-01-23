public class Server {
    private Config config;  

    public Server(Config config) {
        this.config = config;
    }

    public void start() {
        System.out.println("Server started on ports: " + config.ports);
    }
}
