package fr.slama.yeahbot.sql;

/**
 * Created on 23/09/2018.
 */
public class DbCredentials {

    private String host;
    private String username;
    private String password;
    private String databaseName;
    private int port;

    public DbCredentials(String host, String username, String password, String databaseName, int port) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.databaseName = databaseName;
        this.port = port;
    }

    String toURI() {
        return "jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?useUnicode=true&useSSL=false&characterEncoding=utf-8";
    }

    String getUsername() {
        return username;
    }

    String getPassword() {
        return password;
    }
}
