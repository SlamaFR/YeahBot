package fr.slama.yeahbot.commands.core;

import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.utilities.ColorUtil;
import fr.slama.yeahbot.utilities.LanguageUtil;
import fr.slama.yeahbot.utilities.MessageUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Created on 15/03/2018.
 */
public final class BotCommand {

    private final Logger LOGGER = LoggerFactory.getLogger("BotCommand");

    private final String name;
    private final String[] aliases;
    private final boolean displayInHelp;
    private final Command.CommandPermission permission;
    private final Permission[] discordPermission;
    private final Command.CommandCategory category;
    private final Command.CommandExecutor executor;
    private final Object object;
    private final Method method;

    BotCommand(String name, String[] aliases, boolean displayInHelp, Command.CommandPermission permission,
               Permission[] discordPermission, Command.CommandCategory category, Command.CommandExecutor executor,
               Object object, Method method) {
        this.name = name;
        this.aliases = aliases;
        this.displayInHelp = displayInHelp;
        this.permission = permission;
        this.discordPermission = discordPermission;
        this.category = category;
        this.executor = executor;
        this.object = object;
        this.method = method;
    }

    public String getName() {
        return name;
    }

    public String[] getAliases() {
        return aliases;
    }

    public boolean displayInHelp() {
        return displayInHelp;
    }

    public Command.CommandPermission getPermission() {
        return permission;
    }

    public Permission[] getDiscordPermission() {
        return discordPermission;
    }

    public Command.CommandCategory getCategory() {
        return category;
    }

    public Command.CommandExecutor getExecutor() {
        return executor;
    }

    public String getDescription(Guild guild) {
        String value = LanguageUtil.getString(guild, Bundle.DESCRIPTION, name);
        if ("".equals(value)) return LanguageUtil.getString(guild, Bundle.DESCRIPTION, "NONE");
        else return value;
    }

    public String getDescription(String locale) {
        String value = LanguageUtil.getString(locale, Bundle.DESCRIPTION, name);
        if ("".equals(value)) return LanguageUtil.getString(locale, Bundle.DESCRIPTION, "NONE");
        else return value;
    }

    public String[] getArguments(Guild guild) {
        return LanguageUtil.getString(guild, Bundle.ARGUMENTS, name).split("§");
    }

    public String[] getArguments(String locale) {
        return LanguageUtil.getString(locale, Bundle.ARGUMENTS, name).split("§");
    }

    public String[] getArgumentsDescription(Guild guild) {
        return LanguageUtil.getString(guild, Bundle.ARGUMENTS_DESCRIPTION, name).split("§");
    }

    public String[] getArgumentsDescription(String locale) {
        return LanguageUtil.getString(locale, Bundle.ARGUMENTS_DESCRIPTION, name).split("§");
    }

    public String getUsage(Guild guild) {
        return String.format("`%s%s %s`", CommandMap.getPrefix(guild), name, String.join(" ", getArguments(guild)));
    }

    public String getUsage(String locale) {
        return String.format("`%s%s %s`", CommandMap.getPrefix(null), name, String.join(" ", getArguments(locale)));
    }

    Object getObject() {
        return object;
    }

    Method getMethod() {
        return method;
    }

    public void sendUsageEmbed(TextChannel textChannel) {

        try {
            String usageCaption = LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "usage");
            String descriptionCaption = LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "description");
            String categoryCaption = LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "category");
            String permissionCaption = LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "permission");
            String aliasesCaption = LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "aliases");
            String argumentsCaption = LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "arguments");
            String expirationCaption = LanguageUtil.getTimeExpiration(textChannel.getGuild(), 30, TimeUnit.SECONDS);

            String[] arguments = getArguments(textChannel.getGuild());
            String[] argumentsDescription = getArgumentsDescription(textChannel.getGuild());

            if (arguments.length > 0 && !"".equals(arguments[0])) for (int i = 0; i < arguments.length; i++)
                arguments[i] = String.format("● `%s` %s", arguments[i], argumentsDescription[i]);

            EmbedBuilder builder = new EmbedBuilder()
                    .setColor(ColorUtil.ORANGE)
                    .addField(usageCaption, getUsage(textChannel.getGuild()), false)
                    .addField(descriptionCaption, getDescription(textChannel.getGuild()), false)
                    .addField(categoryCaption, LanguageUtil.getString(textChannel.getGuild(), Bundle.CATEGORY, getCategory().getName()), true)
                    .addField(permissionCaption, LanguageUtil.getString(textChannel.getGuild(), Bundle.PERMISSION, getPermission().toString().toLowerCase()), true)
                    .setFooter(expirationCaption, null);

            if (aliases.length > 0) {
                StringBuilder aliasesBuilder = new StringBuilder();
                for (String s : aliases) {
                    if (aliasesBuilder.length() > 1) aliasesBuilder.append(", ");
                    aliasesBuilder.append(String.format("*`%s`*", s));
                }
                builder.addField(aliasesCaption, aliasesBuilder.toString(), true);
            }
            if (arguments.length > 0 && !"".equals(arguments[0]))
                builder.addField(argumentsCaption, String.join("\n", arguments), false);

            textChannel.sendMessage(builder.build()).queue(m -> m.delete().queueAfter(30, TimeUnit.SECONDS));
        } catch (Exception e) {
            LOGGER.error("Error while generating help for " + name + " command!", e);
            textChannel.sendMessage(
                    MessageUtil.getErrorEmbed(textChannel.getGuild(), LanguageUtil.getString(textChannel.getGuild(), Bundle.ERROR, "help_embed"))
            ).queue();
        }

    }

}
