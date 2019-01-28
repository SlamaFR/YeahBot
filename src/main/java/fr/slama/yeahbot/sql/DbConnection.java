package fr.slama.yeahbot.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created on 23/09/2018.
 */
public class DbConnection {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private DbCredentials credentials;
    private Connection connection;

    public DbConnection(DbCredentials credentials) {
        this.credentials = credentials;
        this.connect();
    }

    private void connect() {
        try {
            logger.info("Connecting to database...");

            Class.forName("com.mysql.jdbc.Driver");
            this.connection = DriverManager.getConnection(this.credentials.toURI(), this.credentials.getUsername(), this.credentials.getPassword());
            logger.info("Database successfully connected!");

            Statement checkStatement = getConnection().createStatement();
            checkStatement.addBatch("CREATE TABLE IF NOT EXISTS `guild_channels` (`GUILD_ID` bigint(20) NOT NULL,`CHANNELS` longtext NOT NULL, UNIQUE KEY (`GUILD_ID`)) ENGINE=InnoDB DEFAULT CHARSET=utf8;");
            checkStatement.addBatch("CREATE TABLE IF NOT EXISTS `guild_mutes` (`GUILD_ID` bigint(20) NOT NULL,`MUTES` longtext NOT NULL, UNIQUE KEY (`GUILD_ID`)) ENGINE=InnoDB DEFAULT CHARSET=utf8;");
            checkStatement.addBatch("CREATE TABLE IF NOT EXISTS `guild_reports` (`GUILD_ID` bigint(20) NOT NULL,`REPORTS` longtext NOT NULL, UNIQUE KEY (`GUILD_ID`)) ENGINE=InnoDB DEFAULT CHARSET=utf8;");
            checkStatement.addBatch("CREATE TABLE IF NOT EXISTS `guild_settings` (`GUILD_ID` bigint(20) NOT NULL,`SETTINGS` longtext NOT NULL, UNIQUE KEY (`GUILD_ID`)) ENGINE=InnoDB DEFAULT CHARSET=utf8;");
            checkStatement.addBatch("CREATE TABLE IF NOT EXISTS `guild_playlists` (`GUILD_ID` bigint(20) NOT NULL,`PLAYLISTS` longtext NOT NULL, UNIQUE KEY (`GUILD_ID`)) ENGINE=InnoDB DEFAULT CHARSET=utf8;");
            checkStatement.executeBatch();

            logger.info("Finished initializing database tables!");
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Database connection failed!", e);
            System.exit(100);
        }
    }

    public void close() throws SQLException {
        if (this.connection != null && !this.connection.isClosed()) {
            this.connection.close();
        }
    }

    public Connection getConnection() throws SQLException {
        if (this.connection != null && !this.connection.isClosed()) return this.connection;

        connect();
        return this.connection;
    }

}
