package fr.slama.yeahbot.commands;

import fr.slama.yeahbot.commands.core.BotCommand;
import fr.slama.yeahbot.commands.core.Command;
import fr.slama.yeahbot.commands.core.CommandError;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.language.LanguageUtil;
import fr.slama.yeahbot.listeners.SelectionListener;
import fr.slama.yeahbot.managers.SanctionManager;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.Reports;
import fr.slama.yeahbot.redis.buckets.Settings;
import fr.slama.yeahbot.utilities.ColorUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.exceptions.HierarchyException;
import net.dv8tion.jda.core.requests.RequestFuture;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
            amount = Integer.parseInt(args[0]);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            textChannel.sendMessage(
                    new CommandError(command, command.getArguments(guild)[0], guild, CommandError.ErrorType.INTEGER).toEmbed()
            ).queue();
            return;
        }

        if (amount > 1001 || amount < 1) {
            textChannel.sendMessage(
                    new EmbedBuilder()
                            .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "error"))
                            .setDescription(LanguageUtil.getString(guild, Bundle.ERROR, "message_amount"))
                            .setColor(ColorUtil.RED)
                            .build()
            ).queue();
            return;
        }

        message.delete().queue();

        List<Message> messages = textChannel.getIterableHistory()
                .stream()
                .limit(amount)
                .filter(msg -> message.getMentionedMembers().size() == 0 || message.getMentionedMembers().contains(msg.getMember()))
                .collect(Collectors.toList());

        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "please_wait"))
                .setDescription(LanguageUtil.getString(guild, Bundle.CAPTION, "deleting"))
                .setColor(ColorUtil.ORANGE);

        if (messages.size() > 100)
            builder.setFooter(LanguageUtil.getString(guild, Bundle.STRINGS, "may_take_a_while"), null);

        textChannel.sendMessage(builder.build()).queue(msg -> {
            RequestFuture.allOf(textChannel.purgeMessages(messages)).thenRun(() -> {

                MessageEmbed embed = new EmbedBuilder()
                        .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "success"))
                        .setDescription(LanguageUtil.getArguedString(guild, Bundle.CAPTION, "messages_deleted", messages.size()))
                        .setColor(ColorUtil.GREEN)
                        .build();

                try {
                    msg.editMessage(embed).queue();
                } catch (ErrorResponseException e) {
                    textChannel.sendMessage(embed).queue();
                }
            });
        });

    }

    @Command(name = "mute",
            discordPermission = {Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS},
            permission = Command.CommandPermission.STAFF,
            category = Command.CommandCategory.MODERATION,
            executor = Command.CommandExecutor.USER)
    private void mute(Guild guild, String[] args, Message message, TextChannel textChannel, BotCommand command) {

        if (guild == null) return;

        if (message.getMentionedMembers().isEmpty()) {
            command.sendUsageEmbed(textChannel);
            return;
        }

        int duration;
        TimeUnit unit;

        String[] args1 = Arrays.copyOfRange(args, message.getMentionedMembers().size(), args.length);
        String[] time;

        try {
            time = args1[0].split(":");
            if (time.length != 2) {
                command.sendUsageEmbed(textChannel);
                return;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
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

        for (Member member : message.getMentionedMembers()) {
            SanctionManager.registerMute(member, textChannel, duration, unit);
        }

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

        for (Member member : message.getMentionedMembers()) {
            SanctionManager.unmute(textChannel, member);
        }

    }

    @Command(name = "kick",
            discordPermission = Permission.KICK_MEMBERS,
            permission = Command.CommandPermission.STAFF,
            category = Command.CommandCategory.MODERATION,
            executor = Command.CommandExecutor.USER)
    private void kick(Guild guild, String[] args, Message message, TextChannel textChannel, BotCommand command) {

        if (guild == null) return;

        if (message.getMentionedMembers().isEmpty()) {
            command.sendUsageEmbed(textChannel);
            return;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, message.getMentionedMembers().size(), args.length));

        if (reason.isEmpty()) reason = LanguageUtil.getString(guild, Bundle.STRINGS, "no_reason");

        for (Member member : message.getMentionedMembers()) {
            SanctionManager.registerKick(member, textChannel, reason);
        }

    }

    @Command(name = "ban",
            discordPermission = Permission.BAN_MEMBERS,
            permission = Command.CommandPermission.STAFF,
            category = Command.CommandCategory.MODERATION,
            executor = Command.CommandExecutor.USER)
    private void ban(Guild guild, String[] args, Message message, TextChannel textChannel, BotCommand command) {

        if (guild == null) return;

        if (message.getMentionedMembers().isEmpty()) {
            command.sendUsageEmbed(textChannel);
            return;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, message.getMentionedMembers().size(), args.length));

        if (reason.isEmpty()) reason = LanguageUtil.getString(guild, Bundle.STRINGS, "no_reason");

        for (Member member : message.getMentionedMembers()) {
            SanctionManager.registerBan(member, textChannel, reason);
        }

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
                    new CommandError(
                            cmd,
                            cmd.getArguments(guild)[0],
                            guild,
                            CommandError.ErrorType.MISSING_VALUE
                    ).toEmbed()
            ).queue();
            return;
        }

        textChannel.sendMessage(
                new EmbedBuilder()
                        .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "question"))
                        .setDescription(LanguageUtil.getString(guild, Bundle.STRINGS, "are_you_sure"))
                        .setFooter(LanguageUtil.getString(guild, Bundle.CAPTION, "one_minute_expiration"), null)
                        .build()
        ).queue(msg -> new SelectionListener(
                msg,
                message.getAuthor(),
                60 * 1000,
                SelectionListener.getQuestion(),
                r -> {
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
            try {
                guild.getController().removeRolesFromMember(member, member.getRoles()).queue();
            } catch (HierarchyException ignored) {
            }
        }

        textChannel.sendMessage(
                LanguageUtil.getString(guild, Bundle.STRINGS, "locked")
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
                    new CommandError(cmd, cmd.getArguments(guild)[0], guild, CommandError.ErrorType.MISSING_VALUE).toEmbed()
            ).queue();
        } else {
            Settings settings = RedisData.getSettings(guild);
            switch (args[0]) {
                case "spam":
                    if (args.length == 1) {
                        StringBuilder builder = new StringBuilder();

                        for (Long channel : settings.spamIgnoredChannels) {
                            if (builder.length() > 1) builder.append(", ");
                            builder.append(guild.getTextChannelById(channel).getAsMention());
                        }

                        textChannel.sendMessage(
                                new EmbedBuilder()
                                        .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "spam_ignored_channels"))
                                        .setDescription(builder.length() > 0 ? builder.toString() : LanguageUtil.getString(guild, Bundle.CAPTION, "none"))
                                        .build()
                        ).queue();
                    } else {
                        switch (args[1]) {
                            case "add":
                                if (!message.getMentionedChannels().isEmpty()) {
                                    for (TextChannel channel : message.getMentionedChannels()) {
                                        if (!settings.spamIgnoredChannels.contains(channel.getIdLong()))
                                            settings.spamIgnoredChannels.add(channel.getIdLong());
                                    }
                                    textChannel.sendMessage(
                                            LanguageUtil.getString(guild, Bundle.CAPTION, "setting_updated")
                                    ).queue();
                                } else {
                                    textChannel.sendMessage(
                                            new CommandError(
                                                    cmd,
                                                    cmd.getArguments(guild)[2],
                                                    guild,
                                                    CommandError.ErrorType.MISSING_VALUE
                                            ).toEmbed()
                                    ).queue();
                                    return;
                                }
                                break;
                            case "del":
                                if (!message.getMentionedChannels().isEmpty()) {
                                    for (TextChannel channel : message.getMentionedChannels())
                                        settings.spamIgnoredChannels.remove(channel.getIdLong());
                                    textChannel.sendMessage(
                                            LanguageUtil.getString(guild, Bundle.CAPTION, "setting_updated")
                                    ).queue();
                                } else {
                                    textChannel.sendMessage(
                                            new CommandError(
                                                    cmd,
                                                    cmd.getArguments(guild)[2],
                                                    guild,
                                                    CommandError.ErrorType.MISSING_VALUE
                                            ).toEmbed()
                                    ).queue();
                                    return;
                                }
                                break;
                            default:
                                textChannel.sendMessage(
                                        new CommandError(
                                                cmd,
                                                cmd.getArguments(guild)[1],
                                                guild,
                                                CommandError.ErrorType.MISSING_VALUE
                                        ).toEmbed()
                                ).queue();
                                return;
                        }
                    }
                    break;
                case "swearing":
                    if (args.length == 1) {
                        StringBuilder builder = new StringBuilder();

                        for (Long channel : settings.spamIgnoredChannels) {
                            if (builder.length() > 1) builder.append(", ");
                            builder.append(guild.getTextChannelById(channel).getAsMention());
                        }

                        textChannel.sendMessage(
                                new EmbedBuilder()
                                        .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "swearing_ignored_channels"))
                                        .setDescription(builder.length() > 0 ? builder.toString() : LanguageUtil.getString(guild, Bundle.CAPTION, "none"))
                                        .build()
                        ).queue();
                    } else {
                        switch (args[1]) {
                            case "add":
                                if (!message.getMentionedChannels().isEmpty()) {
                                    for (TextChannel channel : message.getMentionedChannels()) {
                                        if (!settings.swearingIgnoredChannels.contains(channel.getIdLong()))
                                            settings.swearingIgnoredChannels.add(channel.getIdLong());
                                    }
                                    textChannel.sendMessage(
                                            LanguageUtil.getString(guild, Bundle.CAPTION, "setting_updated")
                                    ).queue();
                                } else {
                                    textChannel.sendMessage(
                                            new CommandError(
                                                    cmd,
                                                    cmd.getArguments(guild)[2],
                                                    guild,
                                                    CommandError.ErrorType.MISSING_VALUE
                                            ).toEmbed()
                                    ).queue();
                                    return;
                                }
                                break;
                            case "del":
                                if (!message.getMentionedChannels().isEmpty()) {
                                    for (TextChannel channel : message.getMentionedChannels())
                                        settings.swearingIgnoredChannels.remove(channel.getIdLong());
                                    textChannel.sendMessage(
                                            LanguageUtil.getString(guild, Bundle.CAPTION, "setting_updated")
                                    ).queue();
                                } else {
                                    textChannel.sendMessage(
                                            new CommandError(
                                                    cmd,
                                                    cmd.getArguments(guild)[2],
                                                    guild,
                                                    CommandError.ErrorType.MISSING_VALUE
                                            ).toEmbed()
                                    ).queue();
                                    return;
                                }
                                break;
                            default:
                                textChannel.sendMessage(
                                        new CommandError(
                                                cmd,
                                                cmd.getArguments(guild)[1],
                                                guild,
                                                CommandError.ErrorType.MISSING_VALUE
                                        ).toEmbed()
                                ).queue();
                                return;
                        }
                    }
                    break;
                case "advertising":
                case "ad":
                    if (args.length == 1) {
                        StringBuilder builder = new StringBuilder();

                        for (Long channel : settings.advertisingIgnoredChannels) {
                            if (builder.length() > 1) builder.append(", ");
                            builder.append(guild.getTextChannelById(channel).getAsMention());
                        }

                        textChannel.sendMessage(
                                new EmbedBuilder()
                                        .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "advertising_ignored_channels"))
                                        .setDescription(builder.length() > 0 ? builder.toString() : LanguageUtil.getString(guild, Bundle.CAPTION, "none"))
                                        .build()
                        ).queue();
                    } else {
                        switch (args[1]) {
                            case "add":
                                if (!message.getMentionedChannels().isEmpty()) {
                                    for (TextChannel channel : message.getMentionedChannels()) {
                                        if (!settings.advertisingIgnoredChannels.contains(channel.getIdLong()))
                                            settings.advertisingIgnoredChannels.add(channel.getIdLong());
                                    }
                                    textChannel.sendMessage(
                                            LanguageUtil.getString(guild, Bundle.CAPTION, "setting_updated")
                                    ).queue();
                                } else {
                                    textChannel.sendMessage(
                                            new CommandError(
                                                    cmd,
                                                    cmd.getArguments(guild)[2],
                                                    guild,
                                                    CommandError.ErrorType.MISSING_VALUE
                                            ).toEmbed()
                                    ).queue();
                                    return;
                                }
                                break;
                            case "del":
                                if (!message.getMentionedChannels().isEmpty()) {
                                    for (TextChannel channel : message.getMentionedChannels())
                                        settings.advertisingIgnoredChannels.remove(channel.getIdLong());
                                    textChannel.sendMessage(
                                            LanguageUtil.getString(guild, Bundle.CAPTION, "setting_updated")
                                    ).queue();
                                } else {
                                    textChannel.sendMessage(
                                            new CommandError(
                                                    cmd,
                                                    cmd.getArguments(guild)[2],
                                                    guild,
                                                    CommandError.ErrorType.MISSING_VALUE
                                            ).toEmbed()
                                    ).queue();
                                    return;
                                }
                                break;
                            default:
                                textChannel.sendMessage(
                                        new CommandError(
                                                cmd,
                                                cmd.getArguments(guild)[1],
                                                guild,
                                                CommandError.ErrorType.MISSING_VALUE
                                        ).toEmbed()
                                ).queue();
                                return;
                        }
                    }
                    break;
                default:
                    textChannel.sendMessage(
                            new CommandError(
                                    cmd,
                                    cmd.getArguments(guild)[0],
                                    guild,
                                    CommandError.ErrorType.MISSING_VALUE
                            ).toEmbed()
                    ).queue();
            }
            RedisData.setSettings(guild, settings);
        }

    }
}
