package fr.slama.yeahbot.utilities;

import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.Settings;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Created on 30/09/2018.
 */
public class GuildUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuildUtil.class);

    public static Role getMutedRole(Guild guild, boolean needed) {

        Settings settings = RedisData.getSettings(guild);

        // Looking for the role id in settings.
        if (settings.muteRole > 0 && guild.getRoleById(settings.muteRole) != null) {
            return guild.getRoleById(settings.muteRole);
        } else {
            // Creating role if needed.
            try {
                if (needed) {
                    Role role = guild.createRole()
                            .setName(LanguageUtil.getString(guild, Bundle.CAPTION, "muted"))
                            .setMentionable(false)
                            .complete();
                    for (TextChannel tc : guild.getTextChannels()) {
                        tc.createPermissionOverride(role)
                                .setAllow(Permission.MESSAGE_WRITE, Permission.MESSAGE_READ)
                                .queue();
                        tc.createPermissionOverride(role)
                                .setDeny(Permission.MESSAGE_WRITE, Permission.MESSAGE_READ)
                                .queue();
                    }
                    settings.muteRole = role.getIdLong();
                    RedisData.setSettings(guild, settings);
                    return role;
                }
            } catch (ErrorResponseException | PermissionException e) {
                LOGGER.warn("Unable to create mute role in {}!", guild);
            }
        }

        return null;
    }

    public static TextChannel getLogChannel(Guild guild, boolean needed) {

        Settings settings = RedisData.getSettings(guild);

        if (settings.logChannel > 0 && guild.getTextChannelById(settings.logChannel) != null) {
            return guild.getTextChannelById(settings.logChannel);
        }

        String[] keyWords = {"log", "reports"};

        for (String word : keyWords) {
            Optional<TextChannel> textChannel = guild.getTextChannels().stream().filter(tc -> tc.getName().contains(word)).findFirst();
            if (textChannel.isPresent()) {
                settings.logChannel = textChannel.get().getIdLong();
                RedisData.setSettings(guild, settings);
                return textChannel.get();
            }
        }

        // Creating channel if needed.
        try {
            if (needed) {
                TextChannel channel = guild.createTextChannel("yeahbot-logs").complete();
                channel.createPermissionOverride(guild.getSelfMember())
                        .setAllow(Permission.MESSAGE_WRITE, Permission.MESSAGE_READ)
                        .queue();
                channel.createPermissionOverride(guild.getPublicRole())
                        .setDeny(Permission.MESSAGE_WRITE, Permission.MESSAGE_READ)
                        .queue();
                settings.logChannel = channel.getIdLong();
                RedisData.setSettings(guild, settings);
                return channel;
            }
        } catch (ErrorResponseException | PermissionException e) {
            LOGGER.warn("Unable to create logs channel in {}!", guild);
        }

        return null;
    }

    public static TextChannel getModChannel(Guild guild, boolean needed) {

        Settings settings = RedisData.getSettings(guild);

        // Looking for the channel id in settings.
        if (settings.modChannel > 0 && guild.getTextChannelById(settings.modChannel) != null) {
            return guild.getTextChannelById(settings.modChannel);
        }

        // Looking for the channel by its name.
        String[] keyWords = {"mod", "sanction"};

        for (String word : keyWords) {
            Optional<TextChannel> channel = guild.getTextChannels()
                    .stream()
                    .filter(tc -> tc.getName().contains(word))
                    .findFirst();
            if (channel.isPresent()) {
                settings.modChannel = channel.get().getIdLong();
                RedisData.setSettings(guild, settings);
                return channel.get();
            }
        }

        // Creating channel if needed.
        try {
            if (needed) {
                TextChannel channel = guild.createTextChannel("yeahbot-mod").complete();
                channel.createPermissionOverride(guild.getSelfMember())
                        .setAllow(Permission.MESSAGE_WRITE, Permission.MESSAGE_READ)
                        .queue();
                channel.createPermissionOverride(guild.getPublicRole())
                        .setDeny(Permission.MESSAGE_WRITE, Permission.MESSAGE_READ)
                        .queue();
                settings.modChannel = channel.getIdLong();
                RedisData.setSettings(guild, settings);
                return channel;
            }
        } catch (ErrorResponseException | PermissionException e) {
            LOGGER.warn("Unable to create mod channel in {}!", guild);
        }

        return null;
    }

    public static TextChannel getUpdatesChannel(Guild guild, boolean needed) {

        Settings settings = RedisData.getSettings(guild);

        // Negative id means force not to look for the channel.
        if (settings.updateChannel < 0) return null;

        // Looking for the channel id in settings.
        if (settings.updateChannel > 0 && guild.getTextChannelById(settings.updateChannel) != null) {
            return guild.getTextChannelById(settings.updateChannel);
        }

        // Looking for the channel by its name.
        String[] keyWords = {"update", "mise-a-jour", "mises-a-jour", "mise-à-jour", "mises-à-jour"};

        for (String word : keyWords) {
            Optional<TextChannel> textChannel = guild.getTextChannels()
                    .stream()
                    .filter(tc -> tc.getName().contains(word))
                    .findFirst();
            if (textChannel.isPresent()) {
                settings.updateChannel = textChannel.get().getIdLong();
                RedisData.setSettings(guild, settings);
                return textChannel.get();
            }
        }

        // Creating channel if needed.
        try {
            if (needed) {
                TextChannel channel = guild.createTextChannel("yeahbot-updates").complete();
                channel.createPermissionOverride(guild.getSelfMember())
                        .setAllow(Permission.MESSAGE_WRITE, Permission.MESSAGE_READ)
                        .queue();
                channel.createPermissionOverride(guild.getPublicRole())
                        .setDeny(Permission.MESSAGE_WRITE, Permission.MESSAGE_READ)
                        .queue();
                settings.updateChannel = channel.getIdLong();
                RedisData.setSettings(guild, settings);
                return channel;
            }
        } catch (ErrorResponseException | PermissionException e) {
            LOGGER.warn("Unable to create updates channel in {}!", guild);
        }

        return null;
    }

    public static TextChannel getWelcomeChannel(Guild guild) {

        Settings settings = RedisData.getSettings(guild);

        // Negative id means force not to look for the channel.
        if (settings.joinLeaveChannel < 0) return null;

        // Looking for the channel id in settings.
        if (settings.joinLeaveChannel > 0 && guild.getTextChannelById(settings.joinLeaveChannel) != null) {
            return guild.getTextChannelById(settings.joinLeaveChannel);
        }

        // Looking for the channel by its name.
        String[] keyWords = {"nouveau", "welcome", "bienvenu", "arrivé", "arrive"};

        for (String word : keyWords) {
            Optional<TextChannel> textChannel = guild.getTextChannels()
                    .stream()
                    .filter(tc -> tc.getName().contains(word))
                    .findFirst();
            if (textChannel.isPresent()) {
                settings.joinLeaveChannel = textChannel.get().getIdLong();
                RedisData.setSettings(guild, settings);
                return textChannel.get();
            }
        }

        return null;
    }

}
