package fr.slama.yeahbot.commands;

import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.blub.Paginator;
import fr.slama.yeahbot.commands.core.BotCommand;
import fr.slama.yeahbot.commands.core.Command;
import fr.slama.yeahbot.commands.core.CommandError;
import fr.slama.yeahbot.commands.core.CommandMap;
import fr.slama.yeahbot.json.JSONReader;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.language.Language;
import fr.slama.yeahbot.listeners.SelectionListener;
import fr.slama.yeahbot.managers.SettingsManager;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.Settings;
import fr.slama.yeahbot.utilities.ColorUtil;
import fr.slama.yeahbot.utilities.GuildUtil;
import fr.slama.yeahbot.utilities.LanguageUtil;
import fr.slama.yeahbot.utilities.MessageUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.json.JSONArray;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created on 09/09/2018.
 */
public class Miscellaneous {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

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
                    .setFooter(LanguageUtil.getTimeExpiration(guild, 1, TimeUnit.MINUTES), null);

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
                textChannel.sendMessage(embed.build()).queue(
                        help -> help.delete().queueAfter(1, TimeUnit.MINUTES)
                );
                message.delete().queueAfter(1, TimeUnit.MINUTES);
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
    private void invite(MessageChannel messageChannel, Guild guild) {
        messageChannel.sendMessage(new EmbedBuilder()
                .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "invite_link"))
                .setDescription("[https://invite.yeahbot.net](https://invite.yeahbot.net)")
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
                    LOGGER.info("Leaving " + leavingGuild.toString());
                }
                break;
            case USER:
                if (guild == null) return;

                textChannel.sendMessage(new EmbedBuilder()
                        .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "question"))
                        .setDescription(LanguageUtil.getString(guild, Bundle.STRINGS, "are_you_sure"))
                        .setFooter(LanguageUtil.getTimeExpiration(guild, 1, TimeUnit.MINUTES), null)
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
                                            LOGGER.info("Leaving " + guild.toString());
                                        }
                                    }
                                },
                                false
                        ));
            default:
        }

    }

    @Command(name = "ping")
    private void ping(ShardManager shardManager, Guild guild, TextChannel textChannel, Command.CommandExecutor executor) {

        shardManager.getShards().get(0).getRestPing().queue(ping -> {

            double gatewayPing = shardManager.getAverageGatewayPing();
            double average = (ping + gatewayPing) / 2;

            switch (executor) {
                case USER:
                    Color color = ColorUtil.RED;

                    if (average <= 250) color = ColorUtil.GREEN;
                    if (250 < average && average <= 550) color = ColorUtil.ORANGE;

                    textChannel.sendMessage(new EmbedBuilder()
                            .addField(LanguageUtil.getString(guild, Bundle.CAPTION, "connection_state"),
                                    String.format("HTTP: %sms\nAPI: %sms", ping, gatewayPing), false)
                            .setColor(color)
                            .build()).queue();
                    break;
                case CONSOLE:
                    LOGGER.info(String.format("Ping : %sms", gatewayPing));
                    break;
                default:
                    }
                }
        );

    }

    @Command(name = "toggle",
            displayInHelp = false,
            permission = Command.CommandPermission.OWNER)
    private void toggle(String[] args, TextChannel textChannel) {

        if (args.length == 0) {

            StringBuilder builder = new StringBuilder();
            for (BotCommand command : YeahBot.getInstance().getCommandMap().getDisabledCommands()) {
                if (builder.length() > 1) builder.append(", ");
                builder.append(command.getName());
            }

            LOGGER.info("Currently disabled commands:");
            if (builder.length() > 0) {
                LOGGER.info(builder.toString());
                if (textChannel != null) textChannel.sendMessage(builder.toString()).queue();
            } else LOGGER.warn("None");

        } else {

            if (args[0].equalsIgnoreCase("all")) {

                for (BotCommand command : YeahBot.getInstance().getCommandMap().getRegistry()) {

                    if ("toggle".equals(command.getName())) continue;

                    if (!YeahBot.getInstance().getCommandMap().getDisabledCommands().contains(command)) {
                        YeahBot.getInstance().getCommandMap().getDisabledCommands().add(command);
                        LOGGER.info(command.getName() + " command now disabled");
                    } else {
                        YeahBot.getInstance().getCommandMap().getDisabledCommands().remove(command);
                        LOGGER.info(command.getName() + " command now enabled");
                    }

                }
                return;
            }

            for (String arg : args) {

                BotCommand command = YeahBot.getInstance().getCommandMap().getCommandByName(arg);


                if (command == null) {
                    LOGGER.warn(arg + " command doesn't exist");
                    if (textChannel != null) textChannel.sendMessage(":x:").queue();
                    continue;
                }

                if ("toggle".equals(command.getName())) return;

                if (!YeahBot.getInstance().getCommandMap().getDisabledCommands().contains(command)) {
                    YeahBot.getInstance().getCommandMap().getDisabledCommands().add(command);
                    LOGGER.info(command.getName() + " command now disabled");
                    if (textChannel != null)
                        textChannel.sendMessage(
                                String.format(":regional_indicator_%s::negative_squared_cross_mark:",
                                        command.getName().toLowerCase().charAt(0)
                                )
                        ).queue();
                } else {
                    YeahBot.getInstance().getCommandMap().getDisabledCommands().remove(command);
                    LOGGER.info(command.getName() + " command now enabled");
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

    @Command(name = "prefix",
            aliases = "setprefix",
            permission = Command.CommandPermission.STAFF,
            executor = Command.CommandExecutor.USER)
    private void prefix(Guild guild, TextChannel textChannel, String[] args, BotCommand cmd) {

        if (guild == null) return;

        if (args.length == 0) {
            textChannel.sendMessage(
                    new CommandError(guild, cmd, 0, CommandError.ErrorType.MISSING_VALUE).toEmbed()
            ).queue();
        } else if (args.length == 1) {
            if (args[0].length() > 32) {
                textChannel.sendMessage(
                        new CommandError(guild, cmd, 0, CommandError.ErrorType.TOO_LONG, "32").toEmbed()
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
                    new CommandError(guild, cmd, 0, CommandError.ErrorType.MISSING_VALUE).toEmbed()
            ).queue();
        } else if (args.length == 1) {
            if (Language.has(args[0].toLowerCase())) {
                Settings settings = RedisData.getSettings(guild);
                settings.locale = args[0].toLowerCase();
                RedisData.setSettings(guild, settings);

                textChannel.sendMessage(
                        LanguageUtil.getString(guild, Bundle.CAPTION, "setting_updated")
                ).queue();
            } else {
                textChannel.sendMessage(
                        new CommandError(guild, cmd, 0, CommandError.ErrorType.INCORRECT_VALUE,
                                Language.codeValues()).toEmbed()
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

        new Paginator.Builder<>(textChannel, user, fields)
                .objectName(f -> LanguageUtil.getString(guild, Bundle.SETTINGS, SettingsManager.getSettingKey(f)))
                .listTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "config"))
                .timeout(1, TimeUnit.MINUTES)
                .selectable(true)
                .selectionResult(f -> {
                    try {
                        SettingsManager.editField(guild, textChannel, user, f);
                    } catch (IllegalAccessException e) {
                        SettingsManager.sendErrorEmbed(textChannel);
                    }
                })
                .build();

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
            AtomicInteger count = new AtomicInteger();

            for (Guild guild : YeahBot.getInstance().getShardManager().getGuilds()) {

                Settings settings = RedisData.getSettings(guild);

                JSONObject lang = object.getJSONObject(settings.locale);

                String adds = buildMarkdown(lang.getJSONArray("adds"), "+");
                String removes = buildMarkdown(lang.getJSONArray("removes"), "-");
                String changes = buildMarkdown(lang.getJSONArray("changes"), "#");
                String coming = buildMarkdown(lang.getJSONArray("coming"), "-");

                EmbedBuilder builder = new EmbedBuilder()
                        .setColor(ColorUtil.GREEN)
                        .setTitle(lang.getString("title"))
                        .setDescription(LanguageUtil.getArguedString(guild, Bundle.STRINGS, "current_version",
                                object.getString("version")));

                if (adds.length() > 0)
                    builder.addField(
                            LanguageUtil.getString(guild, Bundle.CAPTION, "adds"),
                            String.format("```diff\n%s```", adds), false
                    );

                if (removes.length() > 0)
                    builder.addField(
                            LanguageUtil.getString(guild, Bundle.CAPTION, "removes"),
                            String.format("```diff\n%s```", removes), false
                    );

                if (changes.length() > 0)
                    builder.addField(
                            LanguageUtil.getString(guild, Bundle.CAPTION, "changes"),
                            String.format("```md\n%s```", changes), false
                    );

                if (coming.length() > 0)
                    builder.addField(
                            LanguageUtil.getString(guild, Bundle.CAPTION, "coming"),
                            String.format("```\n%s```", coming), false
                    );

                builder.addField(
                        LanguageUtil.getString(guild, Bundle.CAPTION, "all_updates"),
                        LanguageUtil.getLink(guild, "https://www.yeahbot.net/updates"),
                        false
                );

                try {
                    TextChannel textChannel = GuildUtil.getUpdatesChannel(guild, Arrays.asList(args).contains("force"));
                    if (textChannel != null) {
                        textChannel.sendMessage(builder.build()).queue();
                        count.getAndIncrement();
                    }
                } catch (PermissionException e) {
                    LOGGER.warn("[Broadcast] Failed to broadcast on {}", guild);
                }

            }

            int total = YeahBot.getInstance().getShardManager().getGuilds().size();

            LOGGER.info("[Broadcast] Sent on {}% guild ({}/{})", (float) count.get() / total * 100, count, total);

        } catch (IOException e) {
            LOGGER.error("Error while fetching data!");
        } catch (JSONException e) {
            LOGGER.error("Error while parsing data!", e);
        }

    }

    @Command(name = "log",
            permission = Command.CommandPermission.STAFF,
            discordPermission = Permission.MANAGE_CHANNEL,
            executor = Command.CommandExecutor.USER)
    private void log(Guild guild, TextChannel textChannel) {

        if (guild == null) return;

        TextChannel logChannel = GuildUtil.getLogChannel(guild, false);

        if (logChannel != null) {
            textChannel.sendMessage(
                    MessageUtil.getWarningEmbed(guild, LanguageUtil.getArguedString(guild, Bundle.ERROR, "log_channel_exists",
                            logChannel.getAsMention()))
            ).queue();
            return;
        }

        logChannel = GuildUtil.getLogChannel(guild, true);

        if (logChannel != null) {
            textChannel.sendMessage(
                    MessageUtil.getSuccessEmbed(guild, LanguageUtil.getArguedString(guild, Bundle.ERROR, "log_channel_created",
                            logChannel.getAsMention()))
            ).queue();
        } else {
            textChannel.sendMessage(
                    MessageUtil.getErrorEmbed(guild, LanguageUtil.getString(guild, Bundle.ERROR, "cannot_create_logs_channel"))
            ).queue();
        }

    }

    @Command(name = "setup",
            permission = Command.CommandPermission.SERVER_OWNER,
            discordPermission = {Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_EMBED_LINKS},
            executor = Command.CommandExecutor.USER)
    private void setup(Guild guild, TextChannel textChannel, Member member) {

        if (!YeahBot.getInstance().getSetupManager().startWizard(textChannel, member)) {
            textChannel.sendMessage(
                    MessageUtil.getErrorEmbed(guild, LanguageUtil.getString(guild, Bundle.ERROR, "setup_already_started"))
            ).queue();
        }

    }

    private String buildMarkdown(JSONArray array, String bullet) {
        StringBuilder builder = new StringBuilder();
        for (Object string : array) {
            if (builder.length() > 1) builder.append("\n\n");
            builder.append(String.format("%s %s", bullet, string));
        }
        return builder.toString();
    }

}
