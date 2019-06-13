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

            textChannel.sendMessage(new EmbedBuilder()
                    .setTitle(String.format("%s | %s",
                            LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "sanction_application"),
                            LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "mute")))
                    .addField(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, target.getUser().isBot() ? "bot" : "user"),
                            String.format("%s#%s", target.getEffectiveName(), target.getUser().getDiscriminator()), true)
                    .addField(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "author"),
                            String.format("%s#%s", author.getEffectiveName(), target.getUser().getDiscriminator()), true)
                    .addField(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "reason"), reason, false)
                    .setFooter(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "until"), null)
                    .setTimestamp(Instant.ofEpochMilli(System.currentTimeMillis() + unit.toMillis(i)))
                    .setColor(ColorUtil.YELLOW)
                    .build()).queue();

            TaskScheduler.scheduleDelayed(() -> unmute(textChannel, target), unit.toMillis(i));

            LOGGER.info(String.format("%s Muted %s", target.getGuild(), target.getUser()));
        } catch (InsufficientPermissionException ignored) {
        }
    }

    public static void unmute(TextChannel textChannel, Member target) {
        Mutes mutes = RedisData.getMutes(target.getGuild());

        if (!mutes.getMutesMap().containsKey(target.getUser().getIdLong())) return;

        textChannel.getGuild().getController()
                .removeRolesFromMember(target, GuildUtil.getMutedRole(target.getGuild(), false))
                .queue();
        mutes.getMutesMap().remove(target.getUser().getIdLong());
        RedisData.setMutes(target.getGuild(), mutes);
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

        textChannel.sendMessage(new EmbedBuilder()
                .setTitle(String.format("%s | %s",
                        LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "sanction_application"),
                        LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "kick")))
                .addField(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, target.getUser().isBot() ? "bot" : "user"),
                        String.format("%s#%s", target.getEffectiveName(), target.getUser().getDiscriminator()), true)
                .addField(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "author"),
                        String.format("%s#%s", author.getEffectiveName(), target.getUser().getDiscriminator()), true)
                .addField(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "reason"), reason, false)
                .setColor(ColorUtil.ORANGE)
                .build()).queue();

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

        textChannel.sendMessage(new EmbedBuilder()
                .setTitle(String.format("%s | %s",
                        LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "sanction_application"),
                        LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "ban")))
                .addField(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, target.getUser().isBot() ? "bot" : "user"),
                        String.format("%s#%s", target.getEffectiveName(), target.getUser().getDiscriminator()), true)
                .addField(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "author"),
                        String.format("%s#%s", author.getEffectiveName(), target.getUser().getDiscriminator()), true)
                .addField(LanguageUtil.getString(target.getGuild(), Bundle.CAPTION, "reason"), reason, false)
                .setColor(ColorUtil.RED)
                .build()).queue();

        LOGGER.info(String.format("%s Banned %s", target.getGuild(), target.getUser()));
    }

}
