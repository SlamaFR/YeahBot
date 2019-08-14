package fr.slama.yeahbot.commands;

import fr.slama.yeahbot.commands.core.BotCommand;
import fr.slama.yeahbot.commands.core.Command;
import fr.slama.yeahbot.commands.core.CommandError;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.listeners.SelectionListener;
import fr.slama.yeahbot.managers.SanctionManager;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.Reports;
import fr.slama.yeahbot.redis.buckets.Settings;
import fr.slama.yeahbot.utilities.ColorUtil;
import fr.slama.yeahbot.utilities.LanguageUtil;
import fr.slama.yeahbot.utilities.MessageUtil;
import fr.slama.yeahbot.utilities.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static fr.slama.yeahbot.commands.core.CommandError.ErrorType.*;

/**
 * Created on 09/09/2018.
 */
public class Moderation {

    @Command(name = "prune",
            discordPermission = Permission.MESSAGE_MANAGE,
            permission = Command.CommandPermission.STAFF,
            category = Command.CommandCategory.MODERATION,
            executor = Command.CommandExecutor.USER)
    private void prune(Guild guild, TextChannel textChannel, Message message, String[] args, BotCommand command) {

        if (guild == null) return;

        if (args.length < 1) {
            command.sendUsageEmbed(textChannel);
            return;
        }

        int amount;

        try {
            amount = Integer.parseInt(args[0]) + 1;
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            textChannel.sendMessage(
                    new CommandError(guild, command, 0, INTEGER).toEmbed()
            ).queue();
            return;
        }

        if (amount > 1001 || amount < 2) {
            textChannel.sendMessage(
                    new CommandError(guild, command, 0, INCORRECT_RANGE, "1", "1000").toEmbed()
            ).queue();
            return;
        }

        List<Message> messages = textChannel.getIterableHistory()
                .stream()
                .limit(amount)
                .filter(msg -> message.getMentionedMembers().size() == 0 || message.getMentionedMembers().contains(msg.getMember()))
                .collect(Collectors.toList());

        if (!messages.contains(message)) messages.add(message);

        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "please_wait"))
                .setDescription(LanguageUtil.getString(guild, Bundle.CAPTION, "deleting"))
                .setColor(ColorUtil.ORANGE);

        if (messages.size() > 100)
            builder.setFooter(LanguageUtil.getString(guild, Bundle.STRINGS, "may_take_a_while"), null);

        textChannel.sendMessage(builder.build()).queue(msg -> {
            CompletableFuture.allOf(textChannel.purgeMessages(messages).toArray(new CompletableFuture[0])).thenRun(() -> {

                MessageEmbed embed = new EmbedBuilder()
                        .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "success"))
                        .setDescription(LanguageUtil.getArguedString(guild, Bundle.CAPTION, "messages_deleted", messages.size() - 1))
                        .setFooter(LanguageUtil.getTimeExpiration(guild, 10, TimeUnit.SECONDS), null)
                        .setColor(ColorUtil.GREEN)
                        .build();

                Consumer<Message> timeout = m -> m.delete().queueAfter(10, TimeUnit.SECONDS);

                try {
                    msg.editMessage(embed).queue(timeout);
                } catch (ErrorResponseException e) {
                    textChannel.sendMessage(embed).queue(timeout);
                }
            });
        });

    }

    @Command(name = "mute",
            discordPermission = {Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS},
            permission = Command.CommandPermission.STAFF,
            category = Command.CommandCategory.MODERATION,
            executor = Command.CommandExecutor.USER)
    private void mute(Guild guild, String[] args, Member member, Message message, TextChannel textChannel, BotCommand command) {

        if (guild == null) return;

        if (message.getMentionedMembers().isEmpty()) {
            command.sendUsageEmbed(textChannel);
            return;
        }

        int duration;
        TimeUnit unit;

        String reason = LanguageUtil.getString(guild, Bundle.STRINGS, "no_reason");
        String[] args1 = Arrays.copyOfRange(args, message.getMentionedMembers().size(), args.length);
        String[] time;

        try {
            time = args1[0].split(":");
            if (time.length != 2) {
                command.sendUsageEmbed(textChannel);
                return;
            }
            if (args1.length > 1) reason = String.join(" ", Arrays.copyOfRange(args1, 1, args1.length));
        } catch (IndexOutOfBoundsException e) {
            command.sendUsageEmbed(textChannel);
            return;
        }

        try {
            duration = Integer.parseInt(time[0]);
        } catch (NumberFormatException e) {
            command.sendUsageEmbed(textChannel);
            return;
        }

        switch (time[1].toLowerCase()) {
            case "s":
                unit = TimeUnit.SECONDS;
                break;
            case "m":
                unit = TimeUnit.MINUTES;
                break;
            case "h":
                unit = TimeUnit.HOURS;
                break;
            case "d":
                unit = TimeUnit.DAYS;
                break;
            default:
                command.sendUsageEmbed(textChannel);
                return;
        }

        reason += String.format(" (%d %s)", duration, LanguageUtil.getTimeUnit(guild, unit, duration));

        boolean success = true;
        for (Member m : message.getMentionedMembers()) {
            if (!SanctionManager.registerMute(member, m, textChannel, reason, duration, unit)) success = false;
        }

        if (success)
            textChannel.sendMessage(MessageUtil.getSuccessEmbed(guild, LanguageUtil.getString(guild, Bundle.STRINGS, "sanction_applied"))).queue();
    }

    @Command(name = "unmute",
            discordPermission = Permission.MANAGE_ROLES,
            permission = Command.CommandPermission.STAFF,
            category = Command.CommandCategory.MODERATION,
            executor = Command.CommandExecutor.USER)
    private void unmute(Guild guild, Message message, TextChannel textChannel, BotCommand command) {

        if (guild == null) return;

        if (message.getMentionedMembers().isEmpty()) {
            command.sendUsageEmbed(textChannel);
            return;
        }

        List<Member> alreadyUnmuted = new ArrayList<>();

        for (Member member : message.getMentionedMembers()) {
            if (!SanctionManager.unregisterMute(textChannel, member))
                alreadyUnmuted.add(member);
        }

        String members = StringUtil.replaceLast(",",
                " " + LanguageUtil.getString(guild, Bundle.CAPTION, "and"),
                String.join(", ", alreadyUnmuted
                        .stream()
                        .map(Member::getAsMention)
                        .collect(Collectors.toSet())));

        String key = "already_unmute" + (alreadyUnmuted.size() > 1 ? "s" : "");

        if (!alreadyUnmuted.isEmpty()) {
            textChannel.sendMessage(
                    new EmbedBuilder()
                            .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "warning"))
                            .setDescription(LanguageUtil.getArguedString(guild, Bundle.STRINGS, key, members))
                            .setColor(ColorUtil.ORANGE)
                            .build()
            ).queue();
        }

    }

    @Command(name = "kick",
            discordPermission = Permission.KICK_MEMBERS,
            permission = Command.CommandPermission.STAFF,
            category = Command.CommandCategory.MODERATION,
            executor = Command.CommandExecutor.USER)
    private void kick(Guild guild, String[] args, Member member, Message message, TextChannel textChannel, BotCommand command) {

        if (guild == null) return;

        if (message.getMentionedMembers().isEmpty()) {
            command.sendUsageEmbed(textChannel);
            return;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, message.getMentionedMembers().size(), args.length));

        if (reason.isEmpty()) reason = LanguageUtil.getString(guild, Bundle.STRINGS, "no_reason");

        boolean success = true;
        for (Member m : message.getMentionedMembers()) {
            if (!SanctionManager.registerKick(member, m, textChannel, reason)) success = false;
        }

        if (success)
            textChannel.sendMessage(MessageUtil.getSuccessEmbed(guild, LanguageUtil.getString(guild, Bundle.STRINGS, "sanction_applied"))).queue();
    }

    @Command(name = "ban",
            discordPermission = Permission.BAN_MEMBERS,
            permission = Command.CommandPermission.STAFF,
            category = Command.CommandCategory.MODERATION,
            executor = Command.CommandExecutor.USER)
    private void ban(Guild guild, String[] args, Member member, Message message, TextChannel textChannel, BotCommand command) {

        if (guild == null) return;

        if (message.getMentionedMembers().isEmpty()) {
            command.sendUsageEmbed(textChannel);
            return;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, message.getMentionedMembers().size(), args.length));

        if (reason.isEmpty()) reason = LanguageUtil.getString(guild, Bundle.STRINGS, "no_reason");

        boolean success = true;
        for (Member m : message.getMentionedMembers()) {
            if (!SanctionManager.registerBan(member, m, textChannel, reason)) success = false;
        }

        if (success)
            textChannel.sendMessage(MessageUtil.getSuccessEmbed(guild, LanguageUtil.getString(guild, Bundle.STRINGS, "sanction_applied"))).queue();
    }

    @Command(name = "clearsins",
            aliases = "cs",
            permission = Command.CommandPermission.STAFF,
            category = Command.CommandCategory.MODERATION,
            executor = Command.CommandExecutor.USER)
    private void clearSins(Guild guild, TextChannel textChannel, Message message, BotCommand cmd) {

        if (guild == null) return;

        if (message.getMentionedMembers().isEmpty()) {
            textChannel.sendMessage(
                    new CommandError(guild, cmd, 0, MISSING_VALUE).toEmbed()
            ).queue();
            return;
        }

        textChannel.sendMessage(
                new EmbedBuilder()
                        .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "question"))
                        .setDescription(LanguageUtil.getString(guild, Bundle.STRINGS, "are_you_sure"))
                        .setFooter(LanguageUtil.getTimeExpiration(guild, 1, TimeUnit.MINUTES), null)
                        .build()
        ).queue(msg -> new SelectionListener(
                msg, message.getAuthor(), 60 * 1000, SelectionListener.getQuestion(), r -> {
                    for (String c : r) {
                        if (c.equals(SelectionListener.getQuestion().get(0))) {
                            textChannel.sendMessage(
                                    LanguageUtil.getString(guild, Bundle.STRINGS, "action_cancelled")
                            ).queue();
                        } else {

                            Reports reports = RedisData.getReports(guild);

                            for (Member member : message.getMentionedMembers()) {
                                reports.getSpamReports().remove(member.getUser().getIdLong());
                                reports.getSwearingReports().remove(member.getUser().getIdLong());
                                reports.getAdvertisingReports().remove(member.getUser().getIdLong());
                            }

                            RedisData.setReports(guild, reports);
                            textChannel.sendMessage(
                                    LanguageUtil.getString(guild, Bundle.STRINGS, "reports_reset")
                            ).queue();
                            return;

                        }
                    }
                },
                false)
        );

    }

    @Command(name = "lock",
            discordPermission = Permission.MANAGE_ROLES,
            permission = Command.CommandPermission.SERVER_OWNER,
            category = Command.CommandCategory.MODERATION,
            executor = Command.CommandExecutor.USER)
    private void lock(Guild guild, TextChannel textChannel) {

        if (guild == null) return;

        for (Member member : guild.getMembers()) {
            if (guild.getSelfMember().getUser().getIdLong() == member.getUser().getIdLong()) continue;
            guild.modifyMemberRoles(member, guild.getPublicRole()).queue();
        }

        textChannel.sendMessage(
                new EmbedBuilder()
                        .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "success"))
                        .setDescription(LanguageUtil.getString(guild, Bundle.STRINGS, "locked"))
                        .setColor(ColorUtil.GREEN)
                        .build()
        ).queue();

    }

    @Command(name = "ignore",
            category = Command.CommandCategory.MODERATION,
            permission = Command.CommandPermission.STAFF,
            executor = Command.CommandExecutor.USER)
    private void ignore(Guild guild, TextChannel textChannel, String[] args, BotCommand cmd, Message message) {

        if (guild == null) return;

        if (args.length == 0) {
            textChannel.sendMessage(
                    new CommandError(guild, cmd, 0, MISSING_VALUE).toEmbed()
            ).queue();
        } else {
            Settings settings = RedisData.getSettings(guild);
            MessageEmbed embed;
            Set<Long> result;
            switch (args[0]) {
                case "spam":
                    embed = new EmbedBuilder()
                            .addField(getField(guild, settings.spamIgnoredChannels, "spam_ignored_channels"))
                            .build();
                    result = getResult(message, args, embed);
                    if (result != null) {
                        if ("add".equals(args[1])) settings.spamIgnoredChannels.addAll(result);
                        if ("del".equals(args[1])) settings.spamIgnoredChannels.removeAll(result);
                        textChannel.sendMessage(
                                MessageUtil.getSuccessEmbed(guild, LanguageUtil.getString(guild, Bundle.CAPTION, "setting_updated"))
                        ).queue();
                    } else {
                        handleError(message, args, cmd);
                    }
                    break;
                case "swearing":
                    embed = new EmbedBuilder()
                            .addField(getField(guild, settings.swearingIgnoredChannels, "swearing_ignored_channels"))
                            .build();
                    result = getResult(message, args, embed);
                    if (result != null) {
                        if ("add".equals(args[1])) settings.swearingIgnoredChannels.addAll(result);
                        if ("del".equals(args[1])) settings.swearingIgnoredChannels.removeAll(result);
                        textChannel.sendMessage(
                                MessageUtil.getSuccessEmbed(guild, LanguageUtil.getString(guild, Bundle.CAPTION, "setting_updated"))
                        ).queue();
                    } else {
                        handleError(message, args, cmd);
                    }
                    break;
                case "advertising":
                case "ad":
                    embed = new EmbedBuilder()
                            .addField(getField(guild, settings.advertisingIgnoredChannels, "advertising_ignored_channels"))
                            .build();
                    result = getResult(message, args, embed);
                    if (result != null) {
                        if ("add".equals(args[1])) settings.advertisingIgnoredChannels.addAll(result);
                        if ("del".equals(args[1])) settings.advertisingIgnoredChannels.removeAll(result);
                        textChannel.sendMessage(
                                MessageUtil.getSuccessEmbed(guild, LanguageUtil.getString(guild, Bundle.CAPTION, "setting_updated"))
                        ).queue();
                    } else {
                        handleError(message, args, cmd);
                    }
                    break;
                case "all":
                    embed = new EmbedBuilder()
                            .addField(getField(guild, settings.spamIgnoredChannels, "spam_ignored_channels"))
                            .addField(getField(guild, settings.swearingIgnoredChannels, "swearing_ignored_channels"))
                            .addField(getField(guild, settings.advertisingIgnoredChannels, "advertising_ignored_channels"))
                            .build();
                    result = getResult(message, args, embed);
                    if (result != null) {
                        if ("add".equals(args[1])) {
                            settings.spamIgnoredChannels.addAll(result);
                            settings.swearingIgnoredChannels.addAll(result);
                            settings.advertisingIgnoredChannels.addAll(result);
                        }
                        if ("del".equals(args[1])) {
                            settings.spamIgnoredChannels.removeAll(result);
                            settings.swearingIgnoredChannels.removeAll(result);
                            settings.advertisingIgnoredChannels.removeAll(result);
                        }
                        textChannel.sendMessage(
                                MessageUtil.getSuccessEmbed(guild, LanguageUtil.getString(guild, Bundle.CAPTION, "setting_updated"))
                        ).queue();
                    } else {
                        handleError(message, args, cmd);
                    }
                    break;
                default:
                    textChannel.sendMessage(
                            new CommandError(guild, cmd, 0, MISSING_VALUE).toEmbed()
                    ).queue();
                    return;
            }
            RedisData.setSettings(guild, settings);
        }

    }

    private Set<Long> getResult(Message message, String[] args, MessageEmbed embed) {
        TextChannel textChannel = message.getTextChannel();
        Set<Long> channels = new HashSet<>();

        if (args.length == 1) {
            textChannel.sendMessage(embed).queue();
            return null;
        } else {
            switch (args[1]) {
                case "add":
                case "del":
                    if (!message.getMentionedChannels().isEmpty()) {
                        for (TextChannel channel : message.getMentionedChannels()) channels.add(channel.getIdLong());
                        return channels;
                    }
                default:
                    return null;
            }
        }
    }

    private void handleError(Message message, String[] args, BotCommand cmd) {
        Guild guild = message.getGuild();
        TextChannel textChannel = message.getTextChannel();

        if (args.length == 2) switch (args[1]) {
            case "add":
            case "del":
                if (message.getMentionedChannels().isEmpty())
                    textChannel.sendMessage(
                            new CommandError(guild, cmd, 2, MISSING_VALUE).toEmbed()
                    ).queue();
                break;
            default:
                textChannel.sendMessage(
                        new CommandError(guild, cmd, 1, MISSING_VALUE).toEmbed()
                ).queue();
        }
    }

    private MessageEmbed.Field getField(Guild guild, Set<Long> channels, String title) {
        StringBuilder builder = new StringBuilder();

        for (Long channel : channels) {
            if (builder.length() > 1) builder.append(", ");
            builder.append(guild.getTextChannelById(channel).getAsMention());
        }
        return new MessageEmbed.Field(LanguageUtil.getString(guild, Bundle.CAPTION, title),
                builder.length() > 0 ? builder.toString() : LanguageUtil.getString(guild, Bundle.CAPTION, "none"),
                false);
    }
}
