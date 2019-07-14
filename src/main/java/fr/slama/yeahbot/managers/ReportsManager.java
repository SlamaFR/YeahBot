package fr.slama.yeahbot.managers;

import fr.slama.yeahbot.blub.SpamType;
import fr.slama.yeahbot.commands.core.Command;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.Reports;
import fr.slama.yeahbot.redis.buckets.Settings;
import fr.slama.yeahbot.utilities.ColorUtil;
import fr.slama.yeahbot.utilities.GuildUtil;
import fr.slama.yeahbot.utilities.LanguageUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.time.Instant;
import java.util.Collections;

/**
 * Created on 28/09/2018.
 */
public class ReportsManager {

    public static void reportSpam(Message message, TextChannel textChannel, SpamType type) {
        Member author = message.getMember();
        long memberId = author.getUser().getIdLong();

        Reports reports = RedisData.getReports(author.getGuild());
        reports.incrSpamReport(author.getUser().getIdLong());

        Settings settings = RedisData.getSettings(author.getGuild());

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setColor(ColorUtil.ORANGE)
                .setTitle(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "report_spam"))
                .addField(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "user"), author.getAsMention(), true)
                .addField(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "text_channel"), textChannel.getAsMention(), true)
                .addField(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "report_amount"), String.format("**`%d`**", reports.getSpamReports().get(memberId)), true)
                .addField(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "type"), LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, type.toKey()), true)
                .addField(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "jump_to_message"),
                        (textChannel.getMessageById(message.getId()) != null
                                ? LanguageUtil.getLink(textChannel.getGuild(), message.getJumpUrl())
                                : String.format("`%s`", LanguageUtil.getString(textChannel.getGuild(), Bundle.ERROR, "message_unavailable"))
                        ), true)
                .setTimestamp(Instant.now());

        String reason = LanguageUtil.getString(author.getGuild(), Bundle.CAPTION, "reason_spam");
        boolean applied = false;

        String warning = type.getSentenceFromSettings(settings);
        if (warning.isEmpty())
            warning = LanguageUtil.getString(textChannel.getGuild(), Bundle.STRINGS, type.toWarningKey());

        if (settings.spamPolicy.containsKey(reports.getSpamReports().get(memberId))) {
            applied = true;
            settings.spamPolicy.get(reports.getSpamReports().get(memberId)).apply(textChannel, author, reason);
        } else if (reports.getSpamReports().get(memberId) > Collections.max(settings.spamPolicy.keySet())) {
            applied = true;
            settings.spamPolicy.get(Collections.max(settings.spamPolicy.keySet())).apply(textChannel, author, reason);
        } else {
            textChannel.sendMessage(new EmbedBuilder()
                    .setColor(ColorUtil.ORANGE)
                    .setAuthor(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "warning"), null, author.getUser().getAvatarUrl())
                    .setDescription(warning.replace("$user", author.getAsMention()))
                    .build()).queue();
        }

        if (applied && (Command.CommandPermission.STAFF.test(author) || !author.getGuild().getSelfMember().canInteract(author)))
            embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_non_applied"), null);

        notify(author, embedBuilder, reports);
    }

    public static void reportSwearing(Message message, TextChannel textChannel) {
        Member author = message.getMember();
        long memberId = author.getUser().getIdLong();

        Reports reports = RedisData.getReports(textChannel.getGuild());
        reports.incrSwearingReport(memberId);

        Settings settings = RedisData.getSettings(textChannel.getGuild());

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setColor(ColorUtil.ORANGE)
                .setTitle(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "report_swearing"))
                .addField(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "user"), author.getAsMention(), true)
                .addField(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "text_channel"), textChannel.getAsMention(), true)
                .addField(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "report_amount"), String.format("**`%d`**", reports.getSwearingReports().get(memberId)), true)
                .addField(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "jump_to_message"),
                        (textChannel.getMessageById(message.getId()) != null
                                ? LanguageUtil.getLink(textChannel.getGuild(), message.getJumpUrl())
                                : String.format("`%s`", LanguageUtil.getString(textChannel.getGuild(), Bundle.ERROR, "message_unavailable"))
                        ), true)
                .setTimestamp(Instant.now());

        String reason = LanguageUtil.getString(author.getGuild(), Bundle.CAPTION, "reason_swearing");
        boolean applied = false;

        String warning = settings.swearingWarningSentence;
        if (warning.isEmpty())
            warning = LanguageUtil.getString(textChannel.getGuild(), Bundle.STRINGS, "swearing_warning_sentence");

        if (settings.swearingPolicy.containsKey(reports.getSwearingReports().get(memberId))) {
            applied = true;
            settings.swearingPolicy.get(reports.getSwearingReports().get(memberId)).apply(textChannel, author, reason);
        } else if (reports.getSwearingReports().get(memberId) > Collections.max(settings.swearingPolicy.keySet())) {
            applied = true;
            settings.swearingPolicy.get(Collections.max(settings.swearingPolicy.keySet())).apply(textChannel, author, reason);
        } else {
            textChannel.sendMessage(new EmbedBuilder()
                    .setColor(ColorUtil.ORANGE)
                    .setAuthor(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "warning"), null, author.getUser().getAvatarUrl())
                    .setDescription(warning.replace("$user", author.getAsMention()))
                    .build()).queue();
        }

        if (applied && (Command.CommandPermission.STAFF.test(author) || !author.getGuild().getSelfMember().canInteract(author)))
            embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_non_applied"), null);

        notify(author, embedBuilder, reports);
    }

    public static void reportAdvertising(Message message, TextChannel textChannel) {
        Member author = message.getGuild().getMember(message.getAuthor());
        long memberId = author.getUser().getIdLong();

        Reports reports = RedisData.getReports(textChannel.getGuild());
        reports.incrAdvertisingReport(memberId);

        Settings settings = RedisData.getSettings(textChannel.getGuild());

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setColor(ColorUtil.ORANGE)
                .setTitle(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "report_advertising"))
                .addField(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "user"), author.getAsMention(), true)
                .addField(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "text_channel"), textChannel.getAsMention(), true)
                .addField(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "report_amount"), String.format("**`%d`**", reports.getAdvertisingReports().get(memberId)), true)
                .addField(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "jump_to_message"),
                        (textChannel.getMessageById(message.getId()) != null
                                ? LanguageUtil.getLink(textChannel.getGuild(), message.getJumpUrl())
                                : String.format("`%s`", LanguageUtil.getString(textChannel.getGuild(), Bundle.ERROR, "message_unavailable"))
                        ), true)
                .setTimestamp(Instant.now());

        String reason = LanguageUtil.getString(author.getGuild(), Bundle.CAPTION, "reason_advertising");
        boolean applied = false;

        String warning = settings.advertisingWarningSentence;
        if (warning.isEmpty())
            warning = LanguageUtil.getString(textChannel.getGuild(), Bundle.STRINGS, "advertising_warning_sentence");

        if (settings.advertisingPolicy.containsKey(reports.getAdvertisingReports().get(memberId))) {
            applied = true;
            settings.advertisingPolicy.get(reports.getAdvertisingReports().get(memberId)).apply(textChannel, author, reason);
        } else if (reports.getAdvertisingReports().get(memberId) > Collections.max(settings.advertisingPolicy.keySet())) {
            applied = true;
            settings.advertisingPolicy.get(Collections.max(settings.advertisingPolicy.keySet())).apply(textChannel, author, reason);
        } else {
            textChannel.sendMessage(new EmbedBuilder()
                    .setColor(ColorUtil.ORANGE)
                    .setAuthor(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "warning"), null, author.getUser().getAvatarUrl())
                    .setDescription(warning.replace("$user", author.getAsMention()))
                    .build()).queue();
        }

        if (applied && (Command.CommandPermission.STAFF.test(author) || !author.getGuild().getSelfMember().canInteract(author)))
            embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_non_applied"), null);

        notify(author, embedBuilder, reports);
    }

    private static void notify(Member author, EmbedBuilder embedBuilder, Reports reports) {
        TextChannel logChannel = GuildUtil.getLogChannel(author.getGuild());

        RedisData.setReports(author.getGuild(), reports);
        if (logChannel != null) logChannel.sendMessage(embedBuilder.build()).queue();
    }

}
