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

    private static <T> String getKey(T t, Guild guild) {
        return String.format("%s:%s", guild.getId(), t.getClass().getSimpleName().toLowerCase());
    }

    private static <T> T getObject(Class<T> tClass, Guild guild) {
        RBucket<T> bucket = RedisAccess.getInstance().getRedissonClient().getBucket(getKey(tClass, guild));
        T settings = bucket.get();
        if (settings == null) {
            try {
                try {
                    PreparedStatement statement = YeahBot.getInstance().getDatabaseManager().getConnection().prepareStatement("SELECT `SETTINGS` FROM `guild_settings` WHERE `GUILD_ID` = (?)");
                    statement.setLong(1, guild.getIdLong());
                    ResultSet resultSet = statement.executeQuery();
                    if (resultSet.next()) settings = new Gson().fromJson(resultSet.getString("SETTINGS"), tClass);
                    else settings = tClass.newInstance();
                } catch (SQLException | NullPointerException e) {
                    settings = tClass.newInstance();
                }
            } catch (IllegalAccessException | InstantiationException ignored) {
            }
            bucket.set(settings);
        }
        return settings;
    }

    private static <T> void setObject(Guild guild, T t) {
        RBucket<T> bucket = RedisAccess.getInstance().getRedissonClient().getBucket(getKey(t, guild));
        bucket.set(t);
    }

    public static Settings getSettings(Guild guild) {
        return getObject(Settings.class, guild);
    }

    public static Reports getReports(Guild guild) {
        return getObject(Reports.class, guild);
    }

    public static Mutes getMutes(Guild guild) {
        return getObject(Mutes.class, guild);
    }

    public static PrivateChannels getPrivateChannels(Guild guild) {
        return getObject(PrivateChannels.class, guild);
    }

    public static Playlists getPlaylists(Guild guild) {
        return getObject(Playlists.class, guild);
    }

    public static void setSettings(Guild guild, Settings settings) {
        setObject(guild, settings);
    }

    public static void setReports(Guild guild, Reports reports) {
        setObject(guild, reports);
    }

    public static void setMutes(Guild guild, Mutes mutes) {
        setObject(guild, mutes);
    }

    public static void setPrivateChannels(Guild guild, PrivateChannels channels) {
        setObject(guild, channels);
    }

    public static void setPlaylists(Guild guild, Playlists playlists) {
        setObject(guild, playlists);
    }

}
