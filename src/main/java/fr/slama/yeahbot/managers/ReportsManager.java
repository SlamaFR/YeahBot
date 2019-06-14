package fr.slama.yeahbot.managers;

import fr.slama.yeahbot.commands.core.Command;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.Reports;
import fr.slama.yeahbot.redis.buckets.Settings;
import fr.slama.yeahbot.utilities.ColorUtil;
import fr.slama.yeahbot.utilities.GuildUtil;
import fr.slama.yeahbot.utilities.LanguageUtil;
import fr.slama.yeahbot.utilities.SpamType;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

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

        TextChannel logChannel = GuildUtil.getLogChannel(author.getGuild());

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setColor(ColorUtil.ORANGE)
                .setTitle(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "report_spam"))
                .addField(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "user"), author.getAsMention(), true)
                .addField(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "text_channel"), textChannel.getAsMention(), true)
                .addField(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "report_amount"), String.format("**`%d`**", reports.getSpamReports().get(memberId)), true)
                .addField(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "type"), LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "spam_" + type.toString()), true)
                .addField(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "jump_to_message"),
                        (textChannel.getMessageById(message.getId()) != null
                                ? LanguageUtil.getLink(textChannel.getGuild(), message.getJumpUrl())
                                : String.format("`%s`", LanguageUtil.getString(textChannel.getGuild(), Bundle.ERROR, "message_unavailable"))
                        ), true)
                .setTimestamp(Instant.now());

        boolean applied = false;
        String reason = LanguageUtil.getString(author.getGuild(), Bundle.CAPTION, "reason_spam");

        switch (reports.getSpamReports().get(memberId)) {
            case 3:
                applied = true;
                embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_applied"), null);
                SanctionManager.registerMute(author.getGuild().getSelfMember(), author, textChannel, reason, 5, TimeUnit.MINUTES);
                break;
            case 5:
                applied = true;
                embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_applied"), null);
                SanctionManager.registerMute(author.getGuild().getSelfMember(), author, textChannel, reason, 30, TimeUnit.MINUTES);
                break;
            case 7:
                applied = true;
                embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_applied"), null);
                SanctionManager.registerMute(author.getGuild().getSelfMember(), author, textChannel, reason, 2, TimeUnit.HOURS);
                break;
            case 10:
                applied = true;
                embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_applied"), null);
                SanctionManager.registerMute(author.getGuild().getSelfMember(), author, textChannel, reason, 24, TimeUnit.HOURS);
                break;
            case 13:
                applied = true;
                embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_applied"), null);
                SanctionManager.registerMute(author.getGuild().getSelfMember(), author, textChannel, reason, 7, TimeUnit.DAYS);
                break;
            case 15:
                applied = true;
                embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_applied"), null);
                embedBuilder.setColor(ColorUtil.ORANGE);
                SanctionManager.registerKick(author.getGuild().getSelfMember(), author, textChannel, LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "reason_spam"));
                break;
            case 17:
                applied = true;
                embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_applied"), null);
                embedBuilder.setColor(ColorUtil.RED);
                SanctionManager.registerBan(author.getGuild().getSelfMember(), author, textChannel, LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "reason_spam"));
                break;
            default:
                if (reports.getSpamReports().get(memberId) > 17) {
                    applied = true;
                    embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_applied"), null);
                    embedBuilder.setColor(ColorUtil.RED);
                    SanctionManager.registerBan(author.getGuild().getSelfMember(), author, textChannel, LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "reason_advertising"));
                    break;
                }
                String warning;
                switch (type) {
                    case CAPS:
                        warning = settings.capsSpamWarningSentence;
                        if (warning.isEmpty())
                            warning = LanguageUtil.getString(textChannel.getGuild(), Bundle.STRINGS, "spam_caps_warning_sentence");
                        textChannel.sendMessage(new EmbedBuilder()
                                .setColor(ColorUtil.ORANGE)
                                .setAuthor(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "warning"), null, author.getUser().getAvatarUrl())
                                .setDescription(warning.replace("$user", author.getAsMention()))
                                .build()).queue();
                        break;
                    case FLOOD:
                        warning = settings.floodWarningSentence;
                        if (warning.isEmpty())
                            warning = LanguageUtil.getString(textChannel.getGuild(), Bundle.STRINGS, "flood_warning_sentence");
                        textChannel.sendMessage(new EmbedBuilder()
                                .setColor(ColorUtil.ORANGE)
                                .setAuthor(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "warning"), null, author.getUser().getAvatarUrl())
                                .setDescription(warning.replace("$user", author.getAsMention()))
                                .build()).queue();
                        break;
                    case EMOJIS:
                        warning = settings.emojisSpamWarningSentence;
                        if (warning.isEmpty())
                            warning = LanguageUtil.getString(textChannel.getGuild(), Bundle.STRINGS, "emojis_spam_warning_sentence");
                        textChannel.sendMessage(new EmbedBuilder()
                                .setColor(ColorUtil.ORANGE)
                                .setAuthor(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "warning"), null, author.getUser().getAvatarUrl())
                                .setDescription(warning.replace("$user", author.getAsMention()))
                                .build()).queue();
                        break;
                    case REACTIONS:
                        warning = settings.reactionsSpamWarningSentence;
                        if (warning.isEmpty())
                            warning = LanguageUtil.getString(textChannel.getGuild(), Bundle.STRINGS, "reactions_spam_warning_sentence");
                        textChannel.sendMessage(new EmbedBuilder()
                                .setColor(ColorUtil.ORANGE)
                                .setAuthor(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "warning"), null, author.getUser().getAvatarUrl())
                                .setDescription(warning.replace("$user", author.getAsMention()))
                                .build()).queue();
                        break;
                }
                break;
        }

        if (applied && (Command.CommandPermission.STAFF.test(author) || author.getGuild().getSelfMember().canInteract(author))) {
            embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_non_applied"), null);
        }

        RedisData.setReports(author.getGuild(), reports);
        if (logChannel != null) logChannel.sendMessage(embedBuilder.build()).queue();

    }

    public static void reportSwearing(Message message, TextChannel textChannel) {

        Member author = message.getMember();
        long memberId = author.getUser().getIdLong();

        Reports reports = RedisData.getReports(textChannel.getGuild());
        reports.incrSwearingReport(memberId);

        Settings settings = RedisData.getSettings(textChannel.getGuild());

        TextChannel logChannel = GuildUtil.getLogChannel(author.getGuild());

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

        boolean applied = false;
        String reason = LanguageUtil.getString(author.getGuild(), Bundle.CAPTION, "reason_swearing");

        switch (reports.getSwearingReports().get(memberId)) {
            case 3:
                applied = true;
                embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_applied"), null);
                SanctionManager.registerMute(author.getGuild().getSelfMember(), author, textChannel, reason, 5, TimeUnit.MINUTES);
                break;
            case 5:
                applied = true;
                embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_applied"), null);
                SanctionManager.registerMute(author.getGuild().getSelfMember(), author, textChannel, reason, 2, TimeUnit.HOURS);
                break;
            case 7:
                applied = true;
                embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_applied"), null);
                SanctionManager.registerMute(author.getGuild().getSelfMember(), author, textChannel, reason, 24, TimeUnit.HOURS);
                break;
            case 8:
                applied = true;
                embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_applied"), null);
                SanctionManager.registerMute(author.getGuild().getSelfMember(), author, textChannel, reason, 7, TimeUnit.DAYS);
                break;
            case 9:
                applied = true;
                embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_applied"), null);
                embedBuilder.setColor(ColorUtil.ORANGE);
                SanctionManager.registerKick(author.getGuild().getSelfMember(), author, textChannel, LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "reason_swearing"));
                break;
            case 10:
                applied = true;
                embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_applied"), null);
                embedBuilder.setColor(ColorUtil.RED);
                SanctionManager.registerBan(author.getGuild().getSelfMember(), author, textChannel, LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "reason_swearing"));
                break;
            default:
                if (reports.getSwearingReports().get(memberId) > 10) {
                    applied = true;
                    embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_applied"), null);
                    embedBuilder.setColor(ColorUtil.RED);
                    SanctionManager.registerBan(author.getGuild().getSelfMember(), author, textChannel, LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "reason_advertising"));
                    break;
                }
                String warning = settings.swearingWarningSentence;
                if (warning.isEmpty())
                    warning = LanguageUtil.getString(textChannel.getGuild(), Bundle.STRINGS, "swearing_warning_sentence");
                textChannel.sendMessage(new EmbedBuilder()
                        .setColor(ColorUtil.ORANGE)
                        .setAuthor(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "warning"), null, author.getUser().getAvatarUrl())
                        .setDescription(warning.replace("$user", author.getAsMention()))
                        .build()).queue();
                break;
        }

        if (applied && (Command.CommandPermission.STAFF.test(author) || author.getGuild().getSelfMember().canInteract(author))) {
            embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_non_applied"), null);
        }

        RedisData.setReports(author.getGuild(), reports);
        if (logChannel != null) logChannel.sendMessage(embedBuilder.build()).queue();

    }

    public static void reportAdvertising(Message message, TextChannel textChannel) {

        Member author = message.getGuild().getMember(message.getAuthor());
        long memberId = author.getUser().getIdLong();

        Reports reports = RedisData.getReports(textChannel.getGuild());
        reports.incrAdvertisingReport(memberId);

        Settings settings = RedisData.getSettings(textChannel.getGuild());

        TextChannel logChannel = GuildUtil.getLogChannel(author.getGuild());

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

        boolean applied = false;
        String reason = LanguageUtil.getString(author.getGuild(), Bundle.CAPTION, "reason_spam");

        switch (reports.getAdvertisingReports().get(memberId)) {
            case 2:
                applied = true;
                embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_applied"), null);
                SanctionManager.registerMute(textChannel.getGuild().getSelfMember(), author, textChannel, reason, 2, TimeUnit.HOURS);
                break;
            case 4:
                applied = true;
                embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_applied"), null);
                embedBuilder.setColor(ColorUtil.ORANGE);
                SanctionManager.registerKick(author.getGuild().getSelfMember(), author, textChannel, LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "reason_advertising"));
                break;
            case 5:
                applied = true;
                embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_applied"), null);
                embedBuilder.setColor(ColorUtil.RED);
                SanctionManager.registerBan(author.getGuild().getSelfMember(), author, textChannel, LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "reason_advertising"));
                break;
            default:
                if (reports.getAdvertisingReports().get(memberId) > 5) {
                    applied = true;
                    embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_applied"), null);
                    embedBuilder.setColor(ColorUtil.RED);
                    SanctionManager.registerBan(author.getGuild().getSelfMember(), author, textChannel, LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "reason_advertising"));
                    break;
                }
                String warning = settings.advertisingWarningSentence;
                if (warning.isEmpty())
                    warning = LanguageUtil.getString(textChannel.getGuild(), Bundle.STRINGS, "advertising_warning_sentence");
                textChannel.sendMessage(new EmbedBuilder()
                        .setColor(ColorUtil.ORANGE)
                        .setAuthor(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "warning"), null, author.getUser().getAvatarUrl())
                        .setDescription(warning.replace("$user", author.getAsMention()))
                        .build()).queue();
                break;
        }

        if (applied && (Command.CommandPermission.STAFF.test(author) || author.getGuild().getSelfMember().canInteract(author))) {
            embedBuilder.setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "sanction_non_applied"), null);
        }

        RedisData.setReports(author.getGuild(), reports);
        if (logChannel != null) logChannel.sendMessage(embedBuilder.build()).queue();

    }

}
