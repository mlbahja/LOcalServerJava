import java.util.List;
import java.util.Map;


public class Config {
    public String host;
    public List<Integer> ports;
    public int clientBodyLimit;
    public Map<Integer, String> errorPages;
    public List<Route> routes;
    public boolean defaultServer;
}
