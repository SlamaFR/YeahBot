package fr.slama.yeahbot.redis;

import com.google.gson.Gson;
import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.redis.buckets.*;
import net.dv8tion.jda.core.entities.Guild;
import org.redisson.api.RBucket;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created on 01/10/2018.
 */
public class RedisData {

    private static String getSettingsKey(Guild guild) {
        return String.format("%s:settings", guild.getId());
    }

    private static String getReportsKey(Guild guild) {
        return String.format("%s:reports", guild.getId());
    }

    private static String getMutesKey(Guild guild) {
        return String.format("%s:mutes", guild.getId());
    }

    private static String getChannelsKey(Guild guild) {
        return String.format("%s:channels", guild.getId());
    }

    private static String getPlaylistsKey(Guild guild) {
        return String.format("%s:playlists", guild.getId());
    }


    public static Settings getSettings(Guild guild) {
        RBucket<Settings> bucket = RedisAccess.INSTANCE.getRedissonClient().getBucket(getSettingsKey(guild));
        Settings settings = bucket.get();
        if (settings == null) {
            try {
                PreparedStatement statement = YeahBot.getInstance().getDatabaseManager().getConnection().prepareStatement("SELECT `SETTINGS` FROM `guild_settings` WHERE `GUILD_ID` = (?)");
                statement.setLong(1, guild.getIdLong());
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) settings = new Gson().fromJson(resultSet.getString("SETTINGS"), Settings.class);
                else settings = new Settings();
            } catch (SQLException | NullPointerException e) {
                settings = new Settings();
            }
            bucket.set(settings);
        }
        return settings;
    }

    public static void setSettings(Guild guild, Settings settings) {
        RBucket<Settings> bucket = RedisAccess.INSTANCE.getRedissonClient().getBucket(getSettingsKey(guild));
        bucket.set(settings);
    }

    public static Reports getReports(Guild guild) {
        RBucket<Reports> bucket = RedisAccess.INSTANCE.getRedissonClient().getBucket(getReportsKey(guild));
        Reports reports = bucket.get();
        if (reports == null) {
            try {
                PreparedStatement statement = YeahBot.getInstance().getDatabaseManager().getConnection().prepareStatement("SELECT `REPORTS` FROM `guild_reports` WHERE `GUILD_ID` = (?)");
                statement.setLong(1, guild.getIdLong());
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) reports = new Gson().fromJson(resultSet.getString("REPORTS"), Reports.class);
                else reports = new Reports();
            } catch (SQLException | NullPointerException e) {
                reports = new Reports();
            }
            bucket.set(reports);
        }
        return reports;
    }

    public static void setReports(Guild guild, Reports reports) {
        RBucket<Reports> bucket = RedisAccess.INSTANCE.getRedissonClient().getBucket(getReportsKey(guild));
        bucket.set(reports);
    }

    public static Mutes getMutes(Guild guild) {
        RBucket<Mutes> bucket = RedisAccess.INSTANCE.getRedissonClient().getBucket(getMutesKey(guild));
        Mutes mutes = bucket.get();
        if (mutes == null) {
            try {
                PreparedStatement statement = YeahBot.getInstance().getDatabaseManager().getConnection().prepareStatement("SELECT `MUTES` FROM `guild_mutes` WHERE `GUILD_ID` = (?)");
                statement.setLong(1, guild.getIdLong());
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) mutes = new Gson().fromJson(resultSet.getString("SETTINGS"), Mutes.class);
                else mutes = new Mutes();
            } catch (SQLException | NullPointerException e) {
                mutes = new Mutes();
            }
            bucket.set(mutes);
        }
        return mutes;
    }

    public static void setMutes(Guild guild, Mutes mutes) {
        RBucket<Mutes> bucket = RedisAccess.INSTANCE.getRedissonClient().getBucket(getMutesKey(guild));
        bucket.set(mutes);
    }

    public static PrivateChannels getPrivateChannels(Guild guild) {
        RBucket<PrivateChannels> bucket = RedisAccess.INSTANCE.getRedissonClient().getBucket(getChannelsKey(guild));
        PrivateChannels channels = bucket.get();
        if (channels == null) {
            try {
                PreparedStatement statement = YeahBot.getInstance().getDatabaseManager().getConnection().prepareStatement("SELECT `CHANNELS` FROM `guild_channels` WHERE `GUILD_ID` = (?)");
                statement.setLong(1, guild.getIdLong());
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next())
                    channels = new Gson().fromJson(resultSet.getString("CHANNELS"), PrivateChannels.class);
                else channels = new PrivateChannels();
            } catch (SQLException | NullPointerException e) {
                channels = new PrivateChannels();
            }
            bucket.set(channels);
        }
        return channels;
    }

    public static void setPrivateChannels(Guild guild, PrivateChannels channels) {
        RBucket<PrivateChannels> bucket = RedisAccess.INSTANCE.getRedissonClient().getBucket(getChannelsKey(guild));
        bucket.set(channels);
    }

    public static Playlists getPlaylists(Guild guild) {
        RBucket<Playlists> bucket = RedisAccess.INSTANCE.getRedissonClient().getBucket(getPlaylistsKey(guild));
        Playlists playlists = bucket.get();
        if (playlists == null) {
            try {
                PreparedStatement statement = YeahBot.getInstance().getDatabaseManager().getConnection().prepareStatement("SELECT `PLAYLISTS` FROM `guild_playlists` WHERE `GUILD_ID` = (?)");
                statement.setLong(1, guild.getIdLong());
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next())
                    playlists = new Gson().fromJson(resultSet.getString("PLAYLISTS"), Playlists.class);
                else playlists = new Playlists();
            } catch (SQLException | NullPointerException e) {
                playlists = new Playlists();
            }
            bucket.set(playlists);
        }
        return playlists;
    }

    public static void setPlaylists(Guild guild, Playlists playlists) {
        RBucket<Playlists> bucket = RedisAccess.INSTANCE.getRedissonClient().getBucket(getPlaylistsKey(guild));
        bucket.set(playlists);
    }

}
