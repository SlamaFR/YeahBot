package fr.slama.yeahbot.utilities;

import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.language.LanguageUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;

/**
 * Created on 2019-05-21.
 */
public class MessageUtil {

    public static void sendPermissionEmbed(Guild guild, TextChannel textChannel, Permission... permission) {
        String perm;
        String title_key = permission.length < 2 ? "permission_needed" : "permissions_needed";
        String key = permission.length < 2 ? "need_permission" : "need_permissions";

        if (permission.length < 2) {
            perm = LanguageUtil.getString(guild, Bundle.PERMISSION, permission[0].toString().toLowerCase());
        } else {
            StringBuilder builder = new StringBuilder();

            for (Permission p : permission) {
                if (builder.length() > 1) builder.append(", ");
                builder.append(LanguageUtil.getString(guild, Bundle.PERMISSION, p.toString().toLowerCase()));
            }
            perm = builder.toString();
        }

        textChannel.sendMessage(
                new EmbedBuilder()
                        .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, title_key))
                        .setDescription(LanguageUtil.getArguedString(guild, Bundle.ERROR, key, perm))
                        .build()
        ).queue();
    }

}
