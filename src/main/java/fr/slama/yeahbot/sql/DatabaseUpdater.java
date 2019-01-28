package fr.slama.yeahbot.sql;

import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.redis.RedisData;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
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

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void run() {

        try {

            PreparedStatement settingsStatement = YeahBot.getInstance().getDatabaseManager().getConnection()
                    .prepareStatement("INSERT INTO `guild_settings` (`GUILD_ID`, `SETTINGS`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `SETTINGS` = (?)");

            for (Guild guild : YeahBot.getInstance().getShardManager().getGuilds()) {

                String value = RedisData.getSettings(guild).toString();

                settingsStatement.setLong(1, guild.getIdLong());
                settingsStatement.setString(2, value);
                settingsStatement.setString(3, value);
                settingsStatement.addBatch();

            }

            settingsStatement.executeBatch();

            PreparedStatement reportsStatement = YeahBot.getInstance().getDatabaseManager().getConnection()
                    .prepareStatement("INSERT INTO `guild_reports` (`GUILD_ID`, `REPORTS`) VALUE (?, ?) ON DUPLICATE KEY UPDATE `REPORTS` = (?)");

            for (Guild guild : YeahBot.getInstance().getShardManager().getGuilds()) {

                String value = RedisData.getReports(guild).toString();

                reportsStatement.setLong(1, guild.getIdLong());
                reportsStatement.setString(2, value);
                reportsStatement.setString(3, value);
                reportsStatement.addBatch();

            }

            reportsStatement.executeBatch();

            PreparedStatement mutesStatement = YeahBot.getInstance().getDatabaseManager().getConnection()
                    .prepareStatement("INSERT INTO `guild_mutes` (`GUILD_ID`, `MUTES`) VALUE (?, ?) ON DUPLICATE KEY UPDATE `MUTES` = (?)");

            for (Guild guild : YeahBot.getInstance().getShardManager().getGuilds()) {

                String value = RedisData.getMutes(guild).toString();

                mutesStatement.setLong(1, guild.getIdLong());
                mutesStatement.setString(2, value);
                mutesStatement.setString(3, value);
                mutesStatement.addBatch();

            }

            mutesStatement.executeBatch();

            PreparedStatement channelsStatement = YeahBot.getInstance().getDatabaseManager().getConnection()
                    .prepareStatement("INSERT INTO `guild_channels` (`GUILD_ID`, `CHANNELS`) VALUE (?, ?) ON DUPLICATE KEY UPDATE `CHANNELS` = (?)");

            for (Guild guild : YeahBot.getInstance().getShardManager().getGuilds()) {

                String value = RedisData.getPrivateChannels(guild).toString();

                channelsStatement.setLong(1, guild.getIdLong());
                channelsStatement.setString(2, value);
                channelsStatement.setString(3, value);
                channelsStatement.addBatch();

            }

            channelsStatement.executeBatch();

            PreparedStatement playlistsStatement = YeahBot.getInstance().getDatabaseManager().getConnection()
                    .prepareStatement("INSERT INTO `guild_playlists` (`GUILD_ID`, `PLAYLISTS`) VALUE (?, ?) ON DUPLICATE KEY UPDATE `PLAYLISTS` = (?)");

            for (Guild guild : YeahBot.getInstance().getShardManager().getGuilds()) {

                String value = RedisData.getPlaylists(guild).toString();

                playlistsStatement.setLong(1, guild.getIdLong());
                playlistsStatement.setString(2, value);
                playlistsStatement.setString(3, value);
                playlistsStatement.addBatch();

            }

            playlistsStatement.executeBatch();
            logger.info("Synchronized database!");

            YeahBot.getInstance().getShardManager().setGame(Game.watching(
                    String.format("%d servers | %d users",
                            YeahBot.getInstance().getShardManager().getGuilds().size(),
                            YeahBot.getInstance().getShardManager().getUsers().stream().filter(u -> !u.isBot()).toArray().length)
            ));
            logger.info("Synchronized presence!");

            if (!YeahBot.isDev()) {
                YeahBot.getInstance().getDiscordBotAPI().setStats(
                        YeahBot.getInstance().getShardManager().getGuilds().size()
                );
                logger.info("Synchronized DBL API!");
            }

        } catch (SQLException e) {
            logger.error("Failed to sync database!", e);
        } catch (NoSuchElementException ignored) {
        }


    }

}
