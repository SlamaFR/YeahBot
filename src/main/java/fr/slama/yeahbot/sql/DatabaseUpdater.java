package fr.slama.yeahbot.sql;

import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.*;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import org.redisson.client.RedisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.TimerTask;

/**
 * Created on 28/09/2018.
 */
public class DatabaseUpdater extends TimerTask {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private <T> String getSQLQuery(Class<T> t) {
        return String.format("INSERT INTO `guild_%1$s` (`GUILD_ID`, `%1$S`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `%1$S` = (?)",
                t.getSimpleName().toLowerCase());
    }

    private <T> PreparedStatement getStatement(Class<T> t) throws SQLException {
        PreparedStatement statement = YeahBot.getInstance().getDatabaseManager().getConnection()
                .prepareStatement(getSQLQuery(t));

        for (Guild guild : YeahBot.getInstance().getShardManager().getGuilds()) {

            String value = RedisData.getObject(t, guild).toString();

            statement.setLong(1, guild.getIdLong());
            statement.setString(2, value);
            statement.setString(3, value);
            statement.addBatch();

        }
        return statement;
    }

    @Override
    public void run() {

        try {

            getStatement(Settings.class).executeBatch();
            getStatement(Reports.class).executeBatch();
            getStatement(Mutes.class).executeBatch();
            getStatement(Channels.class).executeBatch();
            getStatement(Playlists.class).executeBatch();
            LOGGER.info("Synchronized database!");

            if (!YeahBot.isDev()) {
                YeahBot.getInstance().getShardManager().setActivity(Activity.watching(
                        String.format("%d servers | %d users",
                                YeahBot.getInstance().getShardManager().getGuilds().size(),
                                YeahBot.getInstance().getShardManager().getUsers().stream().filter(u -> !u.isBot()).toArray().length)
                ));
                LOGGER.info("Synchronized presence!");

                YeahBot.getInstance().getDiscordBotAPI().setStats(
                        YeahBot.getInstance().getShardManager().getGuilds().size()
                );
                LOGGER.info("Synchronized DBL API!");
            }

        } catch (SQLException | NoSuchElementException | RedisException e) {
            LOGGER.error("Failed to sync database!");
            LOGGER.error(e.getMessage());
        }
    }

}
