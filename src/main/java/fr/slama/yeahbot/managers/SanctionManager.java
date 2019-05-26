package fr.slama.yeahbot.managers;

import fr.slama.yeahbot.commands.core.Command;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.language.LanguageUtil;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.Mutes;
import fr.slama.yeahbot.utilities.ColorUtil;
import fr.slama.yeahbot.utilities.GuildUtil;
import fr.slama.yeahbot.utilities.TaskScheduler;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created on 30/09/2018.
 */
public class SanctionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SanctionManager.class);

    public static void registerMute(Member target, TextChannel textChannel, int i, TimeUnit unit) {
        if (Command.CommandPermission.STAFF.test(target)) {
            textChannel.sendMessage(LanguageUtil.getString(target.getGuild(), Bundle.ERROR, "higher_member")).queue();
            return;
        }

        long timeout = System.currentTimeMillis() + unit.toMillis(i);

        try {
            Mutes mutes = RedisData.getMutes(target.getGuild());
            mutes.getMutesMap().put(target.getUser().getIdLong(), timeout);
            RedisData.setMutes(target.getGuild(), mutes);

            textChannel.getGuild().getController().addRolesToMember(target, GuildUtil.getMutedRole(textChannel.getGuild(), true)).queue();

            textChannel.sendMessage(new EmbedBuilder()
                    .setColor(ColorUtil.RED)
                    .setTitle(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "sanction_application"))
                    .setDescription(LanguageUtil.getArguedString(target.getGuild(), Bundle.STRINGS, "user_muted", target.getAsMention(), i,
                            LanguageUtil.getString(target.getGuild(), Bundle.UNIT, unit.toString().toLowerCase())))
                    .build()).queue();

            TaskScheduler.scheduleRepeating(() -> {
                textChannel.getGuild().getController()
                        .removeRolesFromMember(target, GuildUtil.getMutedRole(target.getGuild(), false))
                        .queue();
                mutes.getMutesMap().remove(target.getUser().getIdLong());
                RedisData.setMutes(target.getGuild(), mutes);
            }, unit.toMillis(i));

            LOGGER.info(String.format("%s Muted %s", target.getGuild(), target.getUser()));
        } catch (InsufficientPermissionException ignored) {
        }
    }

    public static void registerKick(Member target, TextChannel textChannel, String reason) {
        if (!Command.CommandPermission.STAFF.test(target)) {
            target.getGuild().getController().kick(target, reason).queue();

            textChannel.sendMessage(new EmbedBuilder()
                    .setColor(ColorUtil.RED)
                    .setTitle(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "sanction_application"))
                    .setDescription(LanguageUtil.getArguedString(target.getGuild(), Bundle.STRINGS, "user_kicked", target.getAsMention()))
                    .addField(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "reason"), reason, false)
                    .build()).queue();
            LOGGER.info(String.format("%s Kicked %s", target.getGuild(), target.getUser()));
        } else {
            textChannel.sendMessage(LanguageUtil.getString(target.getGuild(), Bundle.ERROR, "higher_member")).queue();
        }
    }

    public static void registerBan(Member target, TextChannel textChannel, String reason) {
        if (!Command.CommandPermission.STAFF.test(target)) {
            target.getGuild().getController().ban(target, 7, reason).queue();

            textChannel.sendMessage(new EmbedBuilder()
                    .setColor(ColorUtil.RED)
                    .setTitle(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "sanction_application"))
                    .setDescription(LanguageUtil.getArguedString(target.getGuild(), Bundle.STRINGS, "user_banned", target.getAsMention()))
                    .addField(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "reason"), reason, false)
                    .build()).queue();
            LOGGER.info(String.format("%s Banned %s", target.getGuild(), target.getUser()));
        } else {
            textChannel.sendMessage(LanguageUtil.getString(target.getGuild(), Bundle.ERROR, "higher_member")).queue();
        }
    }

}
