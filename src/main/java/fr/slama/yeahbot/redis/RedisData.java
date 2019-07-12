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

    private static <T> String getSQLQuery(T t) {
        return String.format("SELECT `%1$S` FROM `guild_%1$s` WHERE `GUILD_ID` = (?)", t.getClass().getSimpleName().toLowerCase());
    }

    private static <T> String getKey(Class<T> tClass, Guild guild) {
        return String.format("%s:%s", guild.getId(), tClass.getSimpleName().toLowerCase());
    }

    public static <T> T getObject(Class<T> tClass, Guild guild) {
        RBucket<T> bucket = RedisAccess.getInstance().getRedissonClient().getBucket(getKey(tClass, guild));
        T t = bucket.get();
        if (t == null) {
            try {
                try {
                    PreparedStatement statement = YeahBot.getInstance().getDatabaseManager().getConnection().prepareStatement(getSQLQuery(tClass));
                    statement.setLong(1, guild.getIdLong());
                    ResultSet resultSet = statement.executeQuery();
                    if (resultSet.next())
                        t = new Gson().fromJson(resultSet.getString(tClass.getSimpleName().toUpperCase()), tClass);
                    else t = tClass.newInstance();
                } catch (SQLException | NullPointerException e) {
                    t = tClass.newInstance();
                }
            } catch (IllegalAccessException | InstantiationException ignored) {
            }
            bucket.set(t);
        }
        return t;
    }

    private static <T> void setObject(Guild guild, T t) {
        RBucket<T> bucket = RedisAccess.getInstance().getRedissonClient().getBucket(getKey(t.getClass(), guild));
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

    public static Channels getPrivateChannels(Guild guild) {
        return getObject(Channels.class, guild);
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

    public static void setPrivateChannels(Guild guild, Channels channels) {
        setObject(guild, channels);
    }

    public static void setPlaylists(Guild guild, Playlists playlists) {
        setObject(guild, playlists);
    }

}
