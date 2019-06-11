package fr.slama.yeahbot.commands;

import fr.slama.yeahbot.commands.core.BotCommand;
import fr.slama.yeahbot.commands.core.Command;
import fr.slama.yeahbot.commands.core.CommandError;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.language.LanguageUtil;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.utilities.ColorUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.Arrays;
import java.util.Random;

/**
 * Created on 09/09/2018.
 */
public class Fun {

    @Command(name = "lmgtfy",
            category = Command.CommandCategory.FUN,
            executor = Command.CommandExecutor.USER)
    private void lmgtfy(Guild guild, TextChannel textChannel, Message message, String[] args, BotCommand cmd) {

        if (args.length < 1) {
            textChannel.sendMessage(new CommandError(cmd, cmd.getArguments(guild)[0], guild, CommandError.ErrorType.MISSING_VALUE).toEmbed()).queue();
            return;
        }

        message.delete().queue();

        String url = String.format("http://%s.lmgtfy.com/?q=%s%s",
                RedisData.getSettings(guild).locale,
                String.join("+", args).replace("-iie", ""),
                Arrays.asList(args).contains("--iie") ? "&iie=1" : "");

        textChannel.sendMessage(new EmbedBuilder()
                .setColor(ColorUtil.PURPLE)
                .setTitle(LanguageUtil.getString(guild, Bundle.STRINGS, "search_ready"))
                .setDescription(LanguageUtil.getLink(guild, url))
                .build()).queue();

    }

    @Command(name = "toss",
            category = Command.CommandCategory.FUN,
            executor = Command.CommandExecutor.USER)
    private void toss(Guild guild, TextChannel textChannel) {

        if (guild == null) return;

        String value = new Random().nextBoolean() ? LanguageUtil.getString(guild, Bundle.CAPTION, "heads") : LanguageUtil.getString(guild, Bundle.CAPTION, "tails");
        textChannel.sendMessage(LanguageUtil.getArguedString(guild, Bundle.STRINGS, "heads_or_tails", value)).queue();

    }

    @Command(name = "rollthedice",
            aliases = "rtd",
            category = Command.CommandCategory.FUN,
            executor = Command.CommandExecutor.USER)
    private void rollTheDice(Guild guild, TextChannel textChannel) {

        if (guild == null) return;

        textChannel.sendMessage(LanguageUtil.getArguedString(guild, Bundle.STRINGS, "roll_the_dice", new Random().nextInt(6) + 1)).queue();

    }

    @Command(name = "random",
            aliases = "rand",
            category = Command.CommandCategory.FUN,
            executor = Command.CommandExecutor.USER)
    private void random(Guild guild, TextChannel textChannel, String[] args, BotCommand cmd) {

        if (guild == null) return;

        if (args.length == 0) {
            textChannel.sendMessage(new CommandError(cmd, cmd.getArguments(guild)[1], guild, CommandError.ErrorType.MISSING_VALUE).toEmbed()).queue();
        } else if (args.length == 1) {
            try {
                int maximum = Integer.parseInt(args[0]);
                textChannel.sendMessage(LanguageUtil.getArguedString(guild, Bundle.STRINGS, "random_number", new Random().nextInt(maximum))).queue();
            } catch (NumberFormatException e) {
                if (Long.parseLong(args[0]) > Integer.MAX_VALUE || Long.parseLong(args[0]) < Integer.MIN_VALUE) {
                    textChannel.sendMessage(
                            new CommandError(cmd, cmd.getArguments(guild)[1], guild, CommandError.ErrorType.INCORRECT_RANGE, String.valueOf(Integer.MIN_VALUE), String.valueOf(Integer.MAX_VALUE)).toEmbed()
                    ).queue();
                } else {
                    textChannel.sendMessage(
                            new CommandError(cmd, cmd.getArguments(guild)[0], guild, CommandError.ErrorType.INTEGER).toEmbed()
                    ).queue();
                }
            }
        } else if (args.length == 2) {
            try {
                int minimum = Integer.parseInt(args[0]);
                int maximum = Integer.parseInt(args[1]);

                if (minimum > maximum) {
                    textChannel.sendMessage(LanguageUtil.getString(guild, Bundle.STRINGS, "check_your_values")).queue();
                    return;
                }
                textChannel.sendMessage(LanguageUtil.getArguedString(guild, Bundle.STRINGS, "random_number", new Random().nextInt(maximum - minimum) + minimum)).queue();
            } catch (NumberFormatException e) {
                if (Long.parseLong(args[0]) > Integer.MAX_VALUE || Long.parseLong(args[0]) < Integer.MIN_VALUE) {
                    textChannel.sendMessage(
                            new CommandError(cmd, cmd.getArguments(guild)[0], guild, CommandError.ErrorType.INCORRECT_RANGE, String.valueOf(Integer.MIN_VALUE), String.valueOf(Integer.MAX_VALUE)).toEmbed()
                    ).queue();
                } else if (Long.parseLong(args[1]) > Integer.MAX_VALUE || Long.parseLong(args[1]) < Integer.MIN_VALUE) {
                    textChannel.sendMessage(
                            new CommandError(cmd, cmd.getArguments(guild)[1], guild, CommandError.ErrorType.INCORRECT_RANGE, String.valueOf(Integer.MIN_VALUE), String.valueOf(Integer.MAX_VALUE)).toEmbed()
                    ).queue();
                } else {
                    textChannel.sendMessage(
                            new CommandError(cmd, cmd.getArguments(guild)[0], guild, CommandError.ErrorType.INTEGER).toEmbed()
                    ).queue();
                }
            }
        } else {
            cmd.sendUsageEmbed(textChannel);
        }

    }

}
