package fr.slama.yeahbot.commands.core;

import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.commands.*;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.utilities.ColorUtil;
import fr.slama.yeahbot.utilities.LanguageUtil;
import fr.slama.yeahbot.utilities.MessageUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * Created on 15/03/2018.
 *
 * @author Slama
 * @version 2.0
 * @since 1.0
 */
public class CommandMap {

    private static final String DEFAULT_PREFIX = "!";
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandMap.class);

    private final Map<String, BotCommand> commands = new HashMap<>();
    private final List<BotCommand> disabledCommand = new ArrayList<>();
    private final List<BotCommand> registry = new ArrayList<>();

    public CommandMap() {
        registerCommands(new Fun());
        registerCommands(new Miscellaneous());
        registerCommands(new Moderation());
        registerCommands(new Music());
        registerCommands(new Util());
    }

    public static String getPrefix(Guild guild) {
        return guild == null ? DEFAULT_PREFIX : RedisData.getSettings(guild).prefix;
    }

    public Collection<String> getCommandNames() {
        Collection<String> names = new HashSet<>();
        for (BotCommand command : registry) {
            names.addAll(Arrays.asList(command.getAliases()));
            names.add(command.getName());
        }
        return names;
    }

    public Collection<BotCommand> getCommands() {
        return commands.values();
    }

    public Collection<BotCommand> getDisabledCommands() {
        return disabledCommand;
    }

    public Collection<BotCommand> getRegistry() {
        return registry;
    }

    private Object[] getCommand(String command) {
        String[] commandSplit = command.split(" ");
        String[] args = new String[commandSplit.length - 1];
        System.arraycopy(commandSplit, 1, args, 0, commandSplit.length - 1);
        BotCommand botCommand = getCommandByName(commandSplit[0]);
        return new Object[]{botCommand, args};
    }

    public BotCommand getCommandByName(String name) {
        return commands.get(name);
    }

    private void registerCommands(Object... objects) {
        for (Object object : objects) registerCommand(object);
    }

    private void registerCommand(Object object) {
        for (Method method : object.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Command.class)) {
                Command command = method.getAnnotation(Command.class);
                method.setAccessible(true);
                BotCommand botCommand;
                botCommand = new BotCommand(command.name(), command.aliases(), command.displayInHelp(), command.permission(), command.discordPermission(), command.category(), command.executor(), object, method);
                for (String name : command.aliases()) commands.put(name, botCommand);
                commands.put(command.name(), botCommand);
                if (!registry.contains(botCommand) && !botCommand.getExecutor().equals(Command.CommandExecutor.CONSOLE))
                    registry.add(botCommand);
            }
        }
        registry.sort(Comparator.comparing(BotCommand::getName));
    }

    public void commandUser(User user, String command, Message message) {
        Object[] object = getCommand(command);
        if (object[0] == null || ((BotCommand) object[0]).getExecutor() == Command.CommandExecutor.CONSOLE)
            return;
        BotCommand cmd = (BotCommand) object[0];
        if (!disabledCommand.contains(object[0])) {
            if (cmd.getPermission().test(message.getGuild().getMember(user))) {
                try {
                    if (message.getGuild().getSelfMember().hasPermission(cmd.getDiscordPermission())) {
                        execute(cmd, command, (String[]) object[1], message, Command.CommandExecutor.USER);
                    } else {
                        MessageUtil.sendPermissionEmbed(message.getGuild(), message.getTextChannel(), cmd.getDiscordPermission());
                    }
                } catch (PermissionException e) {
                    MessageUtil.sendPermissionEmbed(message.getGuild(), message.getTextChannel(), e.getPermission());
                } catch (Exception e) {
                    LOGGER.error("The {} command failed", cmd.getName());
                    if (YeahBot.isDev()) LOGGER.error("Stack Trace:", e);
                    message.getChannel().sendMessage(
                            MessageUtil.getErrorEmbed(message.getGuild(), LanguageUtil.getString(message.getGuild(), Bundle.ERROR, "command"))
                    ).queue();
                }
            } else {
                message.getTextChannel().sendMessage(MessageUtil.getErrorEmbed(message.getGuild(),
                        LanguageUtil.getString(message.getGuild(), Bundle.ERROR, "command"))).queue();
            }
        } else {
            message.getChannel().sendMessage(new EmbedBuilder()
                    .setTitle(LanguageUtil.getString(message.getGuild(), Bundle.CAPTION, "maintenance"))
                    .setDescription(LanguageUtil.getString(message.getGuild(), Bundle.STRINGS, "disabled_command"))
                    .setColor(ColorUtil.RED).build()).queue();
        }
    }

    public void commandConsole(String command) {
        LOGGER.info("[CONSOLE] >> " + command);
        Object[] object = getCommand(command);
        if (object[0] == null) {
            LOGGER.warn("Unknown command.");
            return;
        } else if (((BotCommand) object[0]).getExecutor().equals(Command.CommandExecutor.USER)) {
            LOGGER.warn("User only command!");
            return;
        }
        try {
            execute((BotCommand) object[0], command, (String[]) object[1], null, Command.CommandExecutor.CONSOLE);
        } catch (Exception e) {
            LOGGER.error("An error occurred while executing {} command!", ((BotCommand) object[0]).getName());
            e.printStackTrace();
        }
    }

    private void execute(BotCommand botCommand, String command, String[] args, Message message, Command.CommandExecutor executor) {
        Parameter[] parameters = botCommand.getMethod().getParameters();
        Object[] objects = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getType() == String[].class)
                objects[i] = args;
            else if (parameters[i].getType() == User.class)
                objects[i] = message == null ? null : message.getAuthor();
            else if (parameters[i].getType() == BotCommand.class)
                objects[i] = botCommand;
            else if (parameters[i].getType() == TextChannel.class)
                objects[i] = message == null ? null : message.getTextChannel();
            else if (parameters[i].getType() == PrivateChannel.class)
                objects[i] = message == null ? null : message.getPrivateChannel();
            else if (parameters[i].getType() == Guild.class)
                objects[i] = message == null ? null : message.getGuild();
            else if (parameters[i].getType() == String.class)
                objects[i] = command;
            else if (parameters[i].getType() == Message.class)
                objects[i] = message;
            else if (parameters[i].getType() == Member.class) {
                assert message != null;
                objects[i] = message.getGuild().getMember(message.getAuthor());
            } else if (parameters[i].getType() == ShardManager.class)
                objects[i] = YeahBot.getInstance().getShardManager();
            else if (parameters[i].getType() == Command.CommandExecutor.class)
                objects[i] = executor;
            else if (parameters[i].getType() == MessageChannel.class) {
                objects[i] = message == null ? null : message.getChannel();
            }
        }
        try {
            botCommand.getMethod().invoke(botCommand.getObject(), objects);
        } catch (Exception e) {
            LOGGER.error("An error occurred while executing {} command!", botCommand.getName());
            if (YeahBot.isDev()) e.getCause().printStackTrace();
        }

    }

}
