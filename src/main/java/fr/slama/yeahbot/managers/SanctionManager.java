package fr.slama.yeahbot.managers;

import fr.slama.yeahbot.commands.core.Command;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.Mutes;
import fr.slama.yeahbot.utilities.*;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Created on 30/09/2018.
 */
public class SanctionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SanctionManager.class);

    public static void registerMute(Member author, Member target, TextChannel textChannel, String reason, int i, TimeUnit unit) {
        if (Command.CommandPermission.STAFF.test(target)) {
            textChannel.sendMessage(
                    new EmbedBuilder()
                            .setTitle(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "error"))
                            .setDescription(LanguageUtil.getString(target.getGuild(), Bundle.ERROR, "higher_member"))
                            .setColor(ColorUtil.RED)
                            .build()
            ).queue();
            return;
        }

        long timeout = System.currentTimeMillis() + unit.toMillis(i);

        try {
            Mutes mutes = RedisData.getMutes(target.getGuild());
            mutes.getMutesMap().put(target.getUser().getIdLong(), timeout);
            RedisData.setMutes(target.getGuild(), mutes);

            textChannel.getGuild().getController().addRolesToMember(target, GuildUtil.getMutedRole(textChannel.getGuild(), true)).queue();

            textChannel.sendMessage(
                    getEmbed(author, target, "mute", reason, ColorUtil.YELLOW)
                            .setFooter(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "until"), null)
                            .setTimestamp(Instant.ofEpochMilli(System.currentTimeMillis() + unit.toMillis(i)))
                            .build()
            ).queue();

            TaskScheduler.scheduleDelayed(() -> unregisterMute(textChannel, target), unit.toMillis(i));

            LOGGER.info(String.format("%s Muted %s", target.getGuild(), target.getUser()));
        } catch (InsufficientPermissionException ignored) {
            MessageUtil.sendPermissionEmbed(target.getGuild(), textChannel, Permission.MANAGE_ROLES);
        }
    }

    public static boolean unregisterMute(TextChannel textChannel, Member target) {
        Mutes mutes = RedisData.getMutes(target.getGuild());

        if (!mutes.getMutesMap().containsKey(target.getUser().getIdLong())) return false;

        textChannel.getGuild().getController()
                .removeRolesFromMember(target, GuildUtil.getMutedRole(target.getGuild(), false))
                .queue();
        mutes.getMutesMap().remove(target.getUser().getIdLong());
        RedisData.setMutes(target.getGuild(), mutes);
        return true;
    }

    public static void registerKick(Member author, Member target, TextChannel textChannel, String reason) {
        if (Command.CommandPermission.STAFF.test(target)) {
            textChannel.sendMessage(
                    new EmbedBuilder()
                            .setTitle(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "error"))
                            .setDescription(LanguageUtil.getString(target.getGuild(), Bundle.ERROR, "higher_member"))
                            .setColor(ColorUtil.RED)
                            .build()
            ).queue();
            return;
        }

        target.getGuild().getController().kick(target, reason).queue();

        textChannel.sendMessage(
                getEmbed(author, target, "kick", reason, ColorUtil.ORANGE).build()
        ).queue();

        LOGGER.info(String.format("%s Kicked %s", target.getGuild(), target.getUser()));
    }

    public static void registerBan(Member author, Member target, TextChannel textChannel, String reason) {
        if (Command.CommandPermission.STAFF.test(target)) {
            textChannel.sendMessage(
                    new EmbedBuilder()
                            .setTitle(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "error"))
                            .setDescription(LanguageUtil.getString(target.getGuild(), Bundle.ERROR, "higher_member"))
                            .setColor(ColorUtil.RED)
                            .build()
            ).queue();
            return;
        }

        target.getGuild().getController().ban(target, 7, reason).queue();

        textChannel.sendMessage(
                getEmbed(author, target, "ban", reason, ColorUtil.RED).build()
        ).queue();

        LOGGER.info(String.format("%s Banned %s", target.getGuild(), target.getUser()));
    }

    private static EmbedBuilder getEmbed(Member author, Member target, String type, String reason, Color color) {
        return new EmbedBuilder().setTitle(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, type))
                .addField(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, target.getUser().isBot() ? "bot" : "user"),
                        String.format("%s (%s#%s)", target.getAsMention(), target.getEffectiveName(),
                                target.getUser().getDiscriminator()), true)
                .addField(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "author"),
                        author.getAsMention(), true)
                .addField(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "reason"), reason, false)
                .setColor(color);
    }

}
