package fr.slama.yeahbot.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

import static fr.slama.yeahbot.YeahBot.CONFIG;

/**
 * Created on 23/09/2018.
 */
public class DatabaseManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private DbConnection connection;

    public DatabaseManager() {
        this.connection = new DbConnection(new DbCredentials(CONFIG.database.host, CONFIG.database.username, CONFIG.database.password, CONFIG.database.name, CONFIG.database.port));
    }

    public void close() {
        try {
            logger.info("Closing database connection...");
            this.connection.close();
            logger.info("Database connection successfully closed!");
        } catch (SQLException e) {
            logger.error("Database connection closing failed!", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return connection.getConnection();
    }
}
