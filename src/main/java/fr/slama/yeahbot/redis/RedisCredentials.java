package fr.slama.yeahbot.redis;

/**
 * Created on 16/11/2018.
 */
public class RedisCredentials {

    private String host;
    private String password;
    private int port;
    private String clientName;

    public RedisCredentials(String host, String password, int port, String clientName) {
        this.host = host;
        this.password = password;
        this.port = port;
        this.clientName = clientName;
    }

    public RedisCredentials(String host, String password, int port) {
        this(host, password, port, "yeahbot");
    }

    public String getHost() {
        return host;
    }

    public String getPassword() {
        return password;
    }

    public int getPort() {
        return port;
    }

    public String getClientName() {
        return clientName;
    }

    public String toRedisURL() {
        return "redis://" + host + ":" + port;
    }

}
