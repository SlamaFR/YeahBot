package fr.slama.yeahbot.managers;

import fr.slama.yeahbot.sql.DbConnection;
import fr.slama.yeahbot.sql.DbCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

import static fr.slama.yeahbot.YeahBot.CONFIG;

/**
 * Created on 23/09/2018.
 */
public class DatabaseManager {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private DbConnection connection;

    public DatabaseManager() {
        this.connection = new DbConnection(new DbCredentials(CONFIG.database.host, CONFIG.database.username, CONFIG.database.password, CONFIG.database.name, CONFIG.database.port));
    }

    public void close() {
        try {
            LOGGER.info("Closing database connection...");
            this.connection.close();
            LOGGER.info("Database connection successfully closed!");
        } catch (SQLException e) {
            LOGGER.error("Database connection closing failed!", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return connection.getConnection();
    }
}
