package fr.slama.yeahbot.utilities;

import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.language.LanguageUtil;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.Settings;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;

/**
 * Created on 30/09/2018.
 */
public class GuildUtil {

    public static Role getMutedRole(Guild guild, boolean needed) {

        Settings settings = RedisData.getSettings(guild);
        long id = settings.muteRole;

        if (guild.getRoleById(id) != null) return guild.getRoleById(id);

        Role role;
        try {
            role = guild.getController().createRole()
                    .setName(LanguageUtil.getString(guild, Bundle.CAPTION, "muted"))
                    .setMentionable(false)
                    .setPermissions(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY)
                    .complete();
        } catch (InsufficientPermissionException e) {
            assert guild.getDefaultChannel() != null;
            try {
                if (needed)
                    guild.getDefaultChannel().sendMessage(LanguageUtil.getString(guild, Bundle.ERROR, "cannot_create_mute_role")).queue();
            } catch (InsufficientPermissionException ignored) {
            }
            return null;
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
                } catch (InsufficientPermissionException ignored) {
                }
            }
        }

        settings.muteRole = role.getIdLong();
        RedisData.setSettings(guild, settings);
        return role;

    }

}
