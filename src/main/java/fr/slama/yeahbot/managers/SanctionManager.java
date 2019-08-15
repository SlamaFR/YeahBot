package fr.slama.yeahbot.managers;

import fr.slama.yeahbot.blub.Mute;
import fr.slama.yeahbot.blub.TaskScheduler;
import fr.slama.yeahbot.commands.core.Command;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.Mutes;
import fr.slama.yeahbot.utilities.ColorUtil;
import fr.slama.yeahbot.utilities.GuildUtil;
import fr.slama.yeahbot.utilities.LanguageUtil;
import fr.slama.yeahbot.utilities.MessageUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.awt.*;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Created on 30/09/2018.
 */
public class SanctionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SanctionManager.class);

    public static boolean registerMute(Member author, Member target, TextChannel textChannel, String reason, int i, TimeUnit unit) {
        if (isStaff(textChannel, target)) return false;

        long timeout = System.currentTimeMillis() + unit.toMillis(i);

        try {
            Mutes mutes = RedisData.getMutes(target.getGuild());
            Role role = GuildUtil.getMutedRole(textChannel.getGuild(), true);

            if (role == null) return false;

            textChannel.getGuild().addRoleToMember(target, role).queue();
            TextChannel modChannel = GuildUtil.getModChannel(target.getGuild(), true);
            if (modChannel == null) modChannel = textChannel;

            modChannel.sendMessage(
                    getEmbed(author, target, "mute", reason, ColorUtil.YELLOW)
                            .setFooter(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "until"), null)
                            .setTimestamp(Instant.ofEpochMilli(System.currentTimeMillis() + unit.toMillis(i)))
                            .build()
            ).queue();

            Mute mute = new Mute(author.getIdLong(), timeout, reason);
            TaskScheduler.scheduleDelayed(() -> {
                Mute m = RedisData.getMutes(target.getGuild()).getMutesMap().get(target.getUser().getIdLong());
                if (m.getTargetId() == mute.getTargetId())
                    unregisterMute(author, target, textChannel, String.format("%s (%s)",
                            reason, LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "expiration")));
            }, unit.toMillis(i));
            mutes.getMutesMap().put(target.getUser().getIdLong(), mute);
            RedisData.setMutes(target.getGuild(), mutes);

            LOGGER.info("{} Muted {}", author, target.getUser());
            return true;
        } catch (InsufficientPermissionException e) {
            MessageUtil.sendPermissionEmbed(target.getGuild(), textChannel, Permission.MANAGE_ROLES);
            return false;
        }
    }

    public static boolean unregisterMute(Member author, Member target, @Nullable TextChannel textChannel, String reason) {
        Mutes mutes = RedisData.getMutes(target.getGuild());

        if (!mutes.getMutesMap().containsKey(target.getUser().getIdLong()) &&
                !target.getRoles().contains(GuildUtil.getMutedRole(target.getGuild(), false)))
            return false;

        mutes.getMutesMap().remove(target.getUser().getIdLong());
        RedisData.setMutes(target.getGuild(), mutes);

        Role role = GuildUtil.getMutedRole(target.getGuild(), true);
        if (role == null) return false;

        target.getGuild().removeRoleFromMember(target, role).queue();

        TextChannel modChannel = GuildUtil.getModChannel(target.getGuild(), true);
        if (modChannel == null) modChannel = textChannel;

        if (modChannel != null)
            modChannel.sendMessage(
                    getEmbed(author, target, "unmute", reason, ColorUtil.DARK_GREEN).build()
            ).queue();

        LOGGER.info("{} Unmuted {}", author, target.getUser());
        return true;
    }

    public static boolean registerKick(Member author, Member target, TextChannel textChannel, String reason) {
        if (isStaff(textChannel, target)) return false;

        try {
            target.getGuild().kick(target, reason).queue();

            TextChannel modChannel = GuildUtil.getModChannel(target.getGuild(), true);

            if (modChannel == null) modChannel = textChannel;

            modChannel.sendMessage(
                    getEmbed(author, target, "kick", reason, ColorUtil.ORANGE).build()
            ).queue();

            LOGGER.info("{} Kicked {}", author, target.getUser());
            return true;
        } catch (InsufficientPermissionException e) {
            return false;
        }
    }

    public static boolean registerBan(Member author, Member target, TextChannel textChannel, String reason) {
        if (isStaff(textChannel, target)) return false;

        try {
            target.getGuild().ban(target, 7, reason).queue();

            TextChannel modChannel = GuildUtil.getModChannel(target.getGuild(), true);

            if (modChannel == null) modChannel = textChannel;

            modChannel.sendMessage(
                    getEmbed(author, target, "ban", reason, ColorUtil.RED).build()
            ).queue();

            LOGGER.info("{} Banned {}", author, target.getUser());
            return true;
        } catch (InsufficientPermissionException e) {
            return false;
        }
    }

    private static boolean isStaff(TextChannel textChannel, Member target) {
        if (Command.CommandPermission.STAFF.test(target)) {
            textChannel.sendMessage(
                    MessageUtil.getErrorEmbed(target.getGuild(), LanguageUtil.getString(target.getGuild(), Bundle.ERROR, "higher_member"))
            ).queue();
            return true;
        } else if (target.getIdLong() == target.getGuild().getSelfMember().getIdLong()) {
            textChannel.sendMessage(
                    MessageUtil.getErrorEmbed(target.getGuild(), LanguageUtil.getString(target.getGuild(), Bundle.ERROR, "self_member"))
            ).queue();
            return true;
        }
        return false;
    }

    private static EmbedBuilder getEmbed(Member author, Member target, String type, String reason, Color color) {
        return new EmbedBuilder().setTitle(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, type))
                .addField(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, target.getUser().isBot() ? "bot" : "user"),
                        String.format("%s (%s#%s)", target.getAsMention(), target.getUser().getName(),
                                target.getUser().getDiscriminator()), true)
                .addField(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "author"),
                        author.getAsMention(), true)
                .addField(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "reason"),
                        reason != null ? reason : LanguageUtil.getString(target.getGuild(), Bundle.STRINGS, "no_reason"), false)
                .setColor(color);
    }

}
