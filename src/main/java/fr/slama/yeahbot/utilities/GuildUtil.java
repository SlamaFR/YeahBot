package fr.slama.yeahbot.utilities;

import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.Settings;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Created on 30/09/2018.
 */
public class GuildUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuildUtil.class);

    public static Role getMutedRole(Guild guild, boolean needed) throws InsufficientPermissionException {

        Settings settings = RedisData.getSettings(guild);
        long id = settings.muteRole;

        if (guild.getRoleById(id) != null) return guild.getRoleById(id);

        Role role;
        try {
            role = guild.getController().createRole()
                    .setName(LanguageUtil.getString(guild, Bundle.CAPTION, "muted"))
                    .setMentionable(false)
                    .complete();
        } catch (ErrorResponseException e) {
            assert guild.getDefaultChannel() != null;
            if (e.getErrorCode() == 30005 && needed) {
                guild.getDefaultChannel().sendMessage(LanguageUtil.getString(guild, Bundle.ERROR, "cannot_create_mute_role_max_reached")).queue();
            }
            return null;
        }

        for (TextChannel channel : guild.getTextChannels()) {
            try {
                channel.putPermissionOverride(role)
                        .setDeny(Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_WRITE)
                        .queue();
            } catch (InsufficientPermissionException e) {
                assert guild.getDefaultChannel() != null;
                try {
                    guild.getDefaultChannel().sendMessage(LanguageUtil.getString(guild, Bundle.ERROR, "cannot_create_mute_role")).queue();
                } catch (InsufficientPermissionException e1) {
                    LOGGER.warn("Unable to create mute role in {} and tell error.", guild);
                }
            }
        }

        settings.muteRole = role.getIdLong();
        RedisData.setSettings(guild, settings);
        return role;

    }

    public static TextChannel getLogChannel(Guild guild) {
        if (guild.getTextChannelsByName("yeahbot-logs", true).isEmpty())
            return null;
        return guild.getTextChannelsByName("yeahbot-logs", true).get(0);
    }

    public static TextChannel getModChannel(Guild guild, boolean needed) {
        if (guild.getTextChannelsByName("yeahbot-mod", true).isEmpty()) {
            if (needed) {
                guild.getController().createTextChannel("yeahbot-mod").queue(
                        tc -> {
                            tc.createPermissionOverride(guild.getSelfMember())
                                    .setAllow(Permission.MESSAGE_WRITE)
                                    .queue();
                            tc.createPermissionOverride(guild.getPublicRole())
                                    .setAllow(Permission.MESSAGE_READ)
                                    .setDeny(Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION)
                                    .queue();
                        }
                        , f -> LOGGER.warn("Unable to create mod channel on {}", guild));
            } else return null;
        }
        return guild.getTextChannelsByName("yeahbot-mod", true).get(0);
    }

    public static TextChannel getUpdatesChannel(Guild guild) {

        Settings settings = RedisData.getSettings(guild);

        if (settings.updateChannel > 0) {
            return guild.getTextChannelById(settings.updateChannel);
        }

        String[] keyWords = {"update", "mise-a-jour", "mise-à-jour"};

        for (String word : keyWords) {
            Optional<TextChannel> textChannel = guild.getTextChannels().stream().filter(tc -> tc.getName().contains(word)).findFirst();
            if (textChannel.isPresent()) {
                settings.updateChannel = textChannel.get().getIdLong();
                RedisData.setSettings(guild, settings);
                return textChannel.get();
            }
        }

        return null;
    }

    public static TextChannel getWelcomeChannel(Guild guild) {

        Settings settings = RedisData.getSettings(guild);

        if (settings.joinLeaveChannel > 0) {
            return guild.getTextChannelById(settings.joinLeaveChannel);
        }

        String[] keyWords = {"welcome", "bienvenue", "nouveaux"};

        for (String word : keyWords) {
            Optional<TextChannel> textChannel = guild.getTextChannels().stream().filter(tc -> tc.getName().contains(word)).findFirst();
            if (textChannel.isPresent()) {
                settings.joinLeaveChannel = textChannel.get().getIdLong();
                RedisData.setSettings(guild, settings);
                return textChannel.get();
            }
        }

        return null;
    }

}
