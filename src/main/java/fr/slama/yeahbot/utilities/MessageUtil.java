package fr.slama.yeahbot.utilities;

import fr.slama.yeahbot.language.Bundle;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * Created on 2019-05-21.
 */
public class MessageUtil {

    public static void sendPermissionEmbed(Guild guild, TextChannel textChannel, Permission... permissions) {
        String perm;
        String title_key = permissions.length < 2 ? "permission_needed" : "permissions_needed";
        String key = permissions.length < 2 ? "need_permission" : "need_permissions";

        if (permissions.length < 2) {
            perm = LanguageUtil.getString(guild, Bundle.PERMISSION, permissions[0].toString().toLowerCase());
        } else {
            StringBuilder builder = new StringBuilder();

            for (Permission p : permissions) {
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

    public static MessageEmbed getErrorEmbed(Guild guild, String message) {
        return new EmbedBuilder()
                .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "error"))
                .setDescription(message)
                .setColor(ColorUtil.RED)
                .build();
    }

    public static MessageEmbed getSuccessEmbed(Guild guild, String message) {
        return new EmbedBuilder()
                .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "success"))
                .setDescription(message)
                .setColor(ColorUtil.GREEN)
                .build();
    }

    public static MessageEmbed getWarningEmbed(Guild guild, String message) {
        return new EmbedBuilder()
                .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "warning"))
                .setDescription(message)
                .setColor(ColorUtil.ORANGE)
                .build();
    }
}
