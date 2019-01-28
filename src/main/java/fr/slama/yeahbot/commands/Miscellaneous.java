package fr.slama.yeahbot.commands;

import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.commands.core.BotCommand;
import fr.slama.yeahbot.commands.core.Command;
import fr.slama.yeahbot.commands.core.CommandError;
import fr.slama.yeahbot.commands.core.CommandMap;
import fr.slama.yeahbot.json.JSONReader;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.language.Language;
import fr.slama.yeahbot.language.LanguageUtil;
import fr.slama.yeahbot.listeners.SelectionListener;
import fr.slama.yeahbot.managers.PaginationManager;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.Settings;
import fr.slama.yeahbot.settings.SettingsManager;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created on 09/09/2018.
 */
public class Miscellaneous {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Command(name = "help",
            executor = Command.CommandExecutor.USER)
    private void help(Guild guild, MessageChannel messageChannel, TextChannel textChannel, String[] args, Message message) {

        if (guild == null) {
            messageChannel.sendMessage(new EmbedBuilder()
                    .addField(
                            ":flag_fr: Français",
                            "Merci d'utiliser **YeahBot** depuis un serveur ! Tapez `!invite` pour l'ajouter",
                            false
                    )
                    .addField(
                            ":flag_gb: English",
                            "Make sure to use **YeahBot** from a server! Type `!invite` to add it",
                            false
                    )
                    .build()).queue();
            return;
        }

        if (args.length == 0) {

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "help"))
                    .setDescription(
                            LanguageUtil.getArguedString(guild, Bundle.STRINGS, "help", CommandMap.getPrefix(guild))
                    )
                    .setFooter(LanguageUtil.getString(guild, Bundle.CAPTION, "one_minute_expiration"), null);

            for (Command.CommandCategory category : Command.CommandCategory.values()) {

                StringBuilder builder = new StringBuilder();
                int count = 0;

                for (BotCommand command : YeahBot.getInstance().getCommandMap().getRegistry()) {
                    if (command.getCategory().equals(category) && command.displayInHelp()) {
                        count++;
                        if (builder.length() > 1) builder.append("**,** ");
                        builder.append(String.format("`%s`", command.getName()));
                    }
                }

                String fieldName = String.format("%s %s (%d)",
                        category.getEmote(),
                        LanguageUtil.getString(guild, Bundle.CATEGORY, category.getName()).toUpperCase(),
                        count
                );

                embed.addField(fieldName, builder.toString(), false);
            }

            if (guild.getSelfMember().hasPermission(Permission.MESSAGE_WRITE)) {
                textChannel.sendMessage(
                        new EmbedBuilder()
                                .setColor(new Color(22, 160, 133))
                                .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "important"))
                                .setDescription(LanguageUtil.getString(guild, Bundle.STRINGS, "currently_alpha"))
                                .build()
                ).queue(important -> {
                    textChannel.sendMessage(embed.build()).queue(
                            help -> help.delete().queueAfter(1, TimeUnit.MINUTES)
                    );
                    important.delete().queueAfter(1, TimeUnit.MINUTES);
                    message.delete().queueAfter(1, TimeUnit.MINUTES);
                });
            }

        } else if (args.length == 1) {

            BotCommand command = YeahBot.getInstance().getCommandMap().getCommandByName(args[0]);

            if (command == null || command.getExecutor() == Command.CommandExecutor.CONSOLE) {
                textChannel.sendMessage(LanguageUtil.getString(guild, Bundle.ERROR, "unknown_command")).queue();
                return;
            }

            command.sendUsageEmbed(textChannel);
            message.delete().queueAfter(30, TimeUnit.SECONDS);

        }


    }

    @Command(name = "invite")
    private void invite(MessageChannel messageChannel) {
        messageChannel.sendMessage(new EmbedBuilder()
                .setDescription(messageChannel.getJDA().asBot().getInviteUrl(Permission.toEnumSet(506850422)))
                .build()).queue();
    }

    @Command(name = "leave",
            permission = Command.CommandPermission.STAFF)
    private void leave(ShardManager shardManager, Command.CommandExecutor executor, Guild guild, String[] args, TextChannel textChannel, User user) {

        switch (executor) {
            case CONSOLE:
                if (args.length > 0) for (String arg : args) {
                    Guild leavingGuild = shardManager.getGuildById(arg);
                    leavingGuild.leave().queue();
                    logger.info("Leaving " + leavingGuild.toString());
                }
                break;
            case USER:
                if (guild == null) return;

                textChannel.sendMessage(new EmbedBuilder()
                        .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "question"))
                        .setDescription(LanguageUtil.getString(guild, Bundle.STRINGS, "are_you_sure"))
                        .setFooter(LanguageUtil.getString(guild, Bundle.CAPTION, "one_minute_expiration"), null)
                        .build()).queue(message ->
                        //TODO: rework using EventWaiter
                        new SelectionListener(
                                message,
                                user,
                                60 * 1000,
                                SelectionListener.getQuestion(),
                                r -> {
                                    for (String c : r) {
                                        if (c.equals(SelectionListener.getQuestion().get(0))) {
                                            textChannel.sendMessage(LanguageUtil.getString(guild, Bundle.STRINGS, "action_cancelled")).queue();
                                        } else {
                                            textChannel.sendMessage(LanguageUtil.getString(guild, Bundle.STRINGS, "goodbye")).queue(s -> guild.leave().queue());
                                            logger.info("Leaving " + guild.toString());
                                        }
                                    }
                                },
                                false
                        ));
        }

    }

    @Command(name = "ping")
    private void ping(ShardManager shardManager, Guild guild, TextChannel textChannel, Command.CommandExecutor executor) {

        double ping = shardManager.getAveragePing();

        switch (executor) {
            case USER:
                Color color = new Color(231, 76, 60);
                String state = LanguageUtil.getString(guild, Bundle.CAPTION, "connection_bad");

                if (ping <= 250) {
                    color = new Color(46, 204, 113);
                    state = LanguageUtil.getString(guild, Bundle.CAPTION, "connection_good");
                }
                if (ping > 250 && ping <= 550) {
                    color = new Color(230, 126, 34);
                    state = LanguageUtil.getString(guild, Bundle.CAPTION, "connection_medium");
                }

                textChannel.sendMessage(new EmbedBuilder()
                        .addField(LanguageUtil.getString(guild, Bundle.CAPTION, "connection_state"), String.format("%sms - %s", ping, state), false)
                        .setColor(color)
                        .build()).queue();
                break;
            case CONSOLE:
                logger.info(String.format("Ping : %sms", ping));
                break;
        }


    }

    @Command(name = "fetch",
            displayInHelp = false,
            executor = Command.CommandExecutor.CONSOLE)
    private void fetch(ShardManager shardManager, String[] args) {

        if (args.length == 0) {
            System.out.println("fetch <user|guild|channel>");
            return;
        }

        switch (args[0]) {
            case "user":
                if (args.length > 1) for (int i = 1; i < args.length - 1; i++) {
                    try {
                        System.out.printf("> %s <> %s%n",
                                shardManager.getUserById(args[i]).toString(),
                                shardManager.getUserById(args[i]).getMutualGuilds().toString()
                        );
                    } catch (NumberFormatException e) {
                        System.out.println("Error");
                    }
                }
                break;
            case "guild":
                if (args.length > 1) for (int i = 1; i < args.length - 1; i++) {
                    try {
                        System.out.printf("> %s <> %d - %s%n",
                                shardManager.getGuildById(args[i]).toString(),
                                shardManager.getGuildById(args[i]).getMembers().size(),
                                shardManager.getGuildById(args[i]).getCreationTime()
                        );
                    } catch (NumberFormatException e) {
                        System.out.println("Error");
                    }
                }
                else {
                    System.out.println("> " + shardManager.getGuilds().size());
                    for (Guild guild : shardManager.getGuilds()) {
                        try {
                            System.out.printf("> %s <> %d - Owner: %s%n",
                                    guild.toString(),
                                    guild.getMembers().size(),
                                    guild.getOwner().getUser().toString()
                            );
                        } catch (NumberFormatException e) {
                            System.out.println("Error");
                        }
                    }
                }
                break;
            case "channel":
                if (args.length > 1) for (int i = 1; i < args.length - 1; i++) {
                    try {
                        System.out.printf("> Private: %s%s%n",
                                shardManager.getPrivateChannelById(args[i]),
                                shardManager.getPrivateChannelById(args[i]) != null ? " <> " + shardManager.getPrivateChannelById(args[i]).getCreationTime() + " - " + shardManager.getPrivateChannelById(args[i]).getUser().toString() : ""
                        );
                        System.out.printf("> Text: %s%s%n",
                                shardManager.getTextChannelById(args[i]),
                                shardManager.getTextChannelById(args[i]) != null ? " <> " + shardManager.getTextChannelById(args[i]).getCreationTime() + " - " + shardManager.getTextChannelById(args[i]).getMembers().toString() : ""
                        );
                        System.out.printf("> Voice: %s%s%n",
                                shardManager.getVoiceChannelById(args[i]),
                                shardManager.getVoiceChannelById(args[i]) != null ? " <> " + shardManager.getVoiceChannelById(args[i]).getCreationTime() + " - " + shardManager.getVoiceChannelById(args[i]).getMembers().toString() : ""
                        );
                    } catch (NumberFormatException e) {
                        System.out.println("Error");
                    }
                }
                break;
            default:
                System.out.println("fetch <user|guild|channel>");
                break;
        }

    }

    @Command(name = "toggle",
            displayInHelp = false,
            permission = Command.CommandPermission.OWNER)
    private void toggle(String[] args, TextChannel textChannel) {

        System.out.println(YeahBot.getInstance().getCommandMap().getRegistry().size());

        if (args.length == 0) {

            StringBuilder builder = new StringBuilder();
            for (BotCommand command : YeahBot.getInstance().getCommandMap().getDisabledCommands()) {
                if (builder.length() > 1) builder.append(", ");
                builder.append(command.getName());
            }

            logger.info("Currently disabled commands:");
            if (builder.length() > 0) {
                logger.info(builder.toString());
                if (textChannel != null) textChannel.sendMessage(builder.toString()).queue();
            } else logger.warn("None");

        } else {

            if (args[0].equalsIgnoreCase("all")) {

                for (BotCommand command : YeahBot.getInstance().getCommandMap().getRegistry()) {

                    if (command.getName().equals("toggle")) return;

                    if (!YeahBot.getInstance().getCommandMap().getDisabledCommands().contains(command)) {
                        YeahBot.getInstance().getCommandMap().getDisabledCommands().add(command);
                        logger.info(command.getName() + " command now disabled");
                    } else {
                        YeahBot.getInstance().getCommandMap().getDisabledCommands().remove(command);
                        logger.info(command.getName() + " command now enabled");
                    }

                }
                return;
            }

            for (String arg : args) {

                BotCommand command = YeahBot.getInstance().getCommandMap().getCommandByName(arg);


                if (command == null) {
                    logger.warn(arg + " command doesn't exist");
                    if (textChannel != null) textChannel.sendMessage(":x:").queue();
                    continue;
                }

                if (command.getName().equals("toggle")) return;

                if (!YeahBot.getInstance().getCommandMap().getDisabledCommands().contains(command)) {
                    YeahBot.getInstance().getCommandMap().getDisabledCommands().add(command);
                    logger.info(command.getName() + " command now disabled");
                    if (textChannel != null)
                        textChannel.sendMessage(
                                String.format(":regional_indicator_%s::negative_squared_cross_mark:",
                                        command.getName().toLowerCase().charAt(0)
                                )
                        ).queue();
                } else {
                    YeahBot.getInstance().getCommandMap().getDisabledCommands().remove(command);
                    logger.info(command.getName() + " command now enabled");
                    if (textChannel != null)
                        textChannel.sendMessage(
                                String.format(":regional_indicator_%s::white_check_mark:",
                                        command.getName().toLowerCase().charAt(0)
                                )
                        ).queue();
                }

            }

        }

    }

    @Command(name = "die",
            displayInHelp = false,
            executor = Command.CommandExecutor.CONSOLE)
    private void die() {
        YeahBot.getInstance().setRunning(false);
    }

    @Command(name = "setprefix",
            permission = Command.CommandPermission.STAFF,
            executor = Command.CommandExecutor.USER)
    private void setPrefix(Guild guild, TextChannel textChannel, String[] args, BotCommand cmd) {

        if (guild == null) return;

        if (args.length == 0) {
            textChannel.sendMessage(
                    new CommandError(
                            cmd.getArguments(guild)[0],
                            guild,
                            CommandError.ErrorType.MISSING_VALUE
                    ).toEmbed()
            ).queue();
        } else if (args.length == 1) {
            if (args[0].length() > 32) {
                textChannel.sendMessage(
                        new CommandError(
                                cmd.getArguments(guild)[0],
                                guild,
                                CommandError.ErrorType.TOO_LONG,
                                "32"
                        ).toEmbed()
                ).queue();
                return;
            }
            Settings settings = RedisData.getSettings(guild);
            settings.prefix = args[0];
            RedisData.setSettings(guild, settings);
            textChannel.sendMessage(LanguageUtil.getString(guild, Bundle.CAPTION, "setting_updated")).queue();
        } else {
            cmd.sendUsageEmbed(textChannel);
        }

    }

    @Command(name = "setlocale",
            aliases = {"setlanguage"},
            permission = Command.CommandPermission.STAFF,
            executor = Command.CommandExecutor.USER)
    private void setLocale(Guild guild, TextChannel textChannel, String[] args, BotCommand cmd) {

        if (guild == null) return;

        if (args.length == 0) {
            textChannel.sendMessage(
                    new CommandError(
                            cmd.getArguments(guild)[0],
                            guild,
                            CommandError.ErrorType.MISSING_VALUE
                    ).toEmbed()
            ).queue();
        } else if (args.length == 1) {
            if (Language.languages.contains(args[0].toLowerCase())) {
                Settings settings = RedisData.getSettings(guild);
                settings.locale = args[0].toLowerCase();
                RedisData.setSettings(guild, settings);

                textChannel.sendMessage(
                        LanguageUtil.getString(guild, Bundle.CAPTION, "setting_updated")
                ).queue();
            } else {
                textChannel.sendMessage(
                        new CommandError(
                                cmd.getArguments(guild)[0],
                                guild,
                                CommandError.ErrorType.INCORRECT_VALUE,
                                (String[]) Language.languages.toArray()
                        ).toEmbed()
                ).queue();
            }
        } else {
            cmd.sendUsageEmbed(textChannel);
        }

    }

    @Command(name = "config",
            aliases = {"set", "setting"},
            permission = Command.CommandPermission.STAFF,
            discordPermission = Permission.MESSAGE_ADD_REACTION,
            executor = Command.CommandExecutor.USER)
    private void config(Guild guild, TextChannel textChannel, User user) {

        List<Field> fields = SettingsManager.getFields(guild);

        new PaginationManager.Builder<Field>()
                .textChannel(textChannel)
                .user(user)
                .objectList(fields)
                .objectName(f -> LanguageUtil.getString(guild, Bundle.SETTINGS, SettingsManager.getSettingKey(f)))
                .listTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "config"))
                .timeout(1, TimeUnit.MINUTES)
                .selection(true)
                .selectionResult(f -> {
                    try {
                        SettingsManager.editField(guild, textChannel, user, f);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                })
                .build();

    }

    @Command(name = "ignore",
            permission = Command.CommandPermission.STAFF,
            executor = Command.CommandExecutor.USER)
    private void ignore(Guild guild, TextChannel textChannel, String[] args, BotCommand cmd, Message message) {

        if (guild == null) return;

        if (args.length == 0) {
            textChannel.sendMessage(
                    new CommandError(cmd.getArguments(guild)[0], guild, CommandError.ErrorType.MISSING_VALUE).toEmbed()
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
                                    cmd.getArguments(guild)[0],
                                    guild,
                                    CommandError.ErrorType.MISSING_VALUE
                            ).toEmbed()
                    ).queue();
            }
            RedisData.setSettings(guild, settings);
        }

    }

    @Command(name = "broadcast",
            aliases = {"bc"},
            displayInHelp = false,
            executor = Command.CommandExecutor.CONSOLE)
    private void broadcast(String[] args) {

        if (args.length == 0) return;

        String path = args[0];

        File file = new File(path);

        try {

            JSONReader reader = new JSONReader(file);
            JSONObject object = reader.toJSONObject();

            for (Guild guild : YeahBot.getInstance().getShardManager().getGuilds()) {

                Settings settings = RedisData.getSettings(guild);

                if (!Arrays.asList(args).contains("force") &&
                        (settings.updateChannel == 0 || guild.getTextChannelById(settings.updateChannel) == null))
                    continue;

                JSONObject lang = object.getJSONObject(settings.locale);

                StringBuilder adds = new StringBuilder();
                StringBuilder removes = new StringBuilder();
                StringBuilder changes = new StringBuilder();
                StringBuilder coming = new StringBuilder();

                for (Object string : lang.getJSONArray("adds")) {
                    if (adds.length() > 1) adds.append("\n\n");
                    adds.append(String.format("+ %s", string));
                }

                for (Object string : lang.getJSONArray("removes")) {
                    if (removes.length() > 1) removes.append("\n\n");
                    removes.append(String.format("- %s", string));
                }

                for (Object string : lang.getJSONArray("changes")) {
                    if (changes.length() > 1) changes.append("\n\n");
                    changes.append(String.format("# %s", string));
                }

                for (Object string : lang.getJSONArray("coming")) {
                    if (coming.length() > 1) coming.append("\n\n");
                    coming.append(String.format("- %s", string));
                }

                EmbedBuilder builder = new EmbedBuilder()
                        .setColor(new Color(46, 204, 113))
                        .setTitle(lang.getString("title"))
                        .setDescription(LanguageUtil.getArguedString(guild, Bundle.STRINGS, "current_version",
                                object.getString("version")));

                if (adds.length() > 0)
                    builder.addField(
                            LanguageUtil.getString(guild, Bundle.CAPTION, "adds"),
                            String.format("```diff\n%s```", adds.toString()),
                            false
                    );

                if (removes.length() > 0)
                    builder.addField(
                            LanguageUtil.getString(guild, Bundle.CAPTION, "removes"),
                            String.format("```diff\n%s```", removes.toString()),
                            false
                    );

                if (changes.length() > 0)
                    builder.addField(
                            LanguageUtil.getString(guild, Bundle.CAPTION, "changes"),
                            String.format("```md\n%s```", changes.toString()),
                            false
                    );

                if (coming.length() > 0)
                    builder.addField(
                            LanguageUtil.getString(guild, Bundle.CAPTION, "coming"),
                            String.format("```\n%s```", coming.toString()),
                            false
                    );

                builder.addField(
                        LanguageUtil.getString(guild, Bundle.CAPTION, "all_updates"),
                        String.format("[%s](%s)", LanguageUtil.getString(guild, Bundle.CAPTION, "click_here"), "https://www.yeahbot.net/updates"),
                        false
                );

                try {
                    if (Arrays.asList(args).contains("force") && guild.getDefaultChannel() != null) {
                        guild.getDefaultChannel().sendMessage(builder.build()).queue();
                        return;
                    }
                    guild.getTextChannelById(settings.updateChannel).sendMessage(builder.build()).queue();
                } catch (PermissionException ignored) {
                    logger.warn("[Update] Failed to broadcast on %s", guild);
                }

            }

        } catch (IOException e) {
            logger.error("Error while fetching data!");
        } catch (JSONException e) {
            logger.error("Error while parsing data!", e);
        }

    }

}
