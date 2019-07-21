package fr.slama.yeahbot.managers;

import fr.slama.yeahbot.blub.Sanction;
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
import java.util.Map;

/**
 * Created on 28/09/2018.
 */
public class ReportsManager {

    public static void reportSpam(Message message, SpamType type) {
        TextChannel textChannel = message.getTextChannel();
        Member author = message.getMember();
        long memberId = author.getUser().getIdLong();

        Reports reports = RedisData.getReports(author.getGuild());
        reports.incrSpamReport(author.getUser().getIdLong());

        Settings settings = RedisData.getSettings(author.getGuild());

        EmbedBuilder embedBuilder = getEmbed(message, "report_spam")
                .addField(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "report_amount"),
                        String.format("**`%d`**", reports.getSpamReports().get(memberId)), true)
                .addField(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "type"),
                        LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, type.toKey()), true);
        String reason = LanguageUtil.getString(author.getGuild(), Bundle.CAPTION, "reason_spam");

        String warning = type.getSentenceFromSettings(settings);
        if (warning.isEmpty())
            warning = LanguageUtil.getString(textChannel.getGuild(), Bundle.STRINGS, type.toWarningKey());

        if (applySanction(reports, settings.spamPolicy, message, warning, reason) && cannotInteract(author))
            embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_non_applied"), null);

        notify(author, embedBuilder, reports);
    }

    public static void reportSwearing(Message message) {
        TextChannel textChannel = message.getTextChannel();
        Member author = message.getMember();
        long memberId = author.getUser().getIdLong();

        Reports reports = RedisData.getReports(textChannel.getGuild());
        reports.incrSwearingReport(memberId);

        Settings settings = RedisData.getSettings(textChannel.getGuild());

        EmbedBuilder embedBuilder = getEmbed(message, "report_swearing")
                .addField(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "report_amount"),
                        String.format("**`%d`**", reports.getSwearingReports().get(memberId)), true);
        String reason = LanguageUtil.getString(author.getGuild(), Bundle.CAPTION, "reason_swearing");

        String warning = settings.swearingWarningSentence;
        if (warning.isEmpty())
            warning = LanguageUtil.getString(textChannel.getGuild(), Bundle.STRINGS, "swearing_warning_sentence");

        if (applySanction(reports, settings.swearingPolicy, message, warning, reason) && cannotInteract(author))
            embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_non_applied"), null);

        notify(author, embedBuilder, reports);
    }

    public static void reportAdvertising(Message message) {
        TextChannel textChannel = message.getTextChannel();
        Member author = message.getMember();
        long memberId = author.getUser().getIdLong();

        Reports reports = RedisData.getReports(textChannel.getGuild());
        reports.incrAdvertisingReport(memberId);

        Settings settings = RedisData.getSettings(textChannel.getGuild());

        EmbedBuilder embedBuilder = getEmbed(message, "report_advertising")
                .addField(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "report_amount"),
                        String.format("**`%d`**", reports.getAdvertisingReports().get(memberId)), true);
        String reason = LanguageUtil.getString(author.getGuild(), Bundle.CAPTION, "reason_advertising");

        String warning = settings.advertisingWarningSentence;
        if (warning.isEmpty())
            warning = LanguageUtil.getString(textChannel.getGuild(), Bundle.STRINGS, "advertising_warning_sentence");

        if (applySanction(reports, settings.advertisingPolicy, message, warning, reason) && cannotInteract(author))
            embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_non_applied"), null);

        notify(author, embedBuilder, reports);
    }

    private static EmbedBuilder getEmbed(Message message, String type) {
        TextChannel textChannel = message.getTextChannel();
        Member author = message.getMember();

        return new EmbedBuilder()
                .setColor(ColorUtil.ORANGE)
                .setTitle(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, type))
                .addField(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "user"), author.getAsMention(), true)
                .addField(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "text_channel"), textChannel.getAsMention(), true)
                .addField(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "jump_to_message"),
                        (textChannel.getMessageById(message.getId()) != null
                                ? LanguageUtil.getLink(textChannel.getGuild(), message.getJumpUrl())
                                : String.format("`%s`", LanguageUtil.getString(textChannel.getGuild(), Bundle.ERROR, "message_unavailable"))
                        ), true)
                .setTimestamp(Instant.now());
    }

    private static boolean applySanction(Reports reports, Map<Integer, Sanction> policy, Message message, String warning, String reason) {
        TextChannel textChannel = message.getTextChannel();
        Member author = message.getMember();
        long memberId = author.getUser().getIdLong();

        boolean applied = false;

        if (policy.containsKey(reports.getSpamReports().get(memberId))) {
            applied = true;
            policy.get(reports.getSpamReports().get(memberId)).apply(textChannel, author, reason);
        } else if (reports.getSpamReports().get(memberId) > Collections.max(policy.keySet())) {
            applied = true;
            policy.get(Collections.max(policy.keySet())).apply(textChannel, author, reason);
        } else {
            textChannel.sendMessage(new EmbedBuilder()
                    .setColor(ColorUtil.ORANGE)
                    .setAuthor(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "warning"), null,
                            author.getUser().getAvatarUrl())
                    .setDescription(warning.replace("$user", author.getAsMention()))
                    .build()).queue();
        }
        return applied;
    }

    private static void notify(Member author, EmbedBuilder embedBuilder, Reports reports) {
        TextChannel logChannel = GuildUtil.getLogChannel(author.getGuild());

        RedisData.setReports(author.getGuild(), reports);
        if (logChannel != null) logChannel.sendMessage(embedBuilder.build()).queue();
    }

    private static boolean cannotInteract(Member member) {
        return Command.CommandPermission.STAFF.test(member) || !member.getGuild().getSelfMember().canInteract(member);
    }

}
