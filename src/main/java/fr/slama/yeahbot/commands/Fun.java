package fr.slama.yeahbot.commands;

import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.commands.core.BotCommand;
import fr.slama.yeahbot.commands.core.Command;
import fr.slama.yeahbot.commands.core.CommandError;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.utilities.ColorUtil;
import fr.slama.yeahbot.utilities.LanguageUtil;
import fr.slama.yeahbot.utilities.MessageUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created on 09/09/2018.
 */
public class Fun {

    @Command(name = "lmgtfy",
            category = Command.CommandCategory.FUN,
            executor = Command.CommandExecutor.USER)
    private void lmgtfy(Guild guild, TextChannel textChannel, Message message, String[] args, BotCommand cmd) {

        if (args.length < 1) {
            textChannel.sendMessage(new CommandError(guild, cmd, 0, CommandError.ErrorType.MISSING_VALUE).toEmbed()).queue();
            return;
        }

        message.delete().queue();
        boolean iie = Arrays.asList(args).contains("--iie") || Arrays.asList(args).contains("-i");

        String url = String.format("http://%s.lmgtfy.com/?q=%s%s",
                RedisData.getSettings(guild).locale,
                String.join("+", args).replace("-iie", ""),
                iie ? "&iie=1" : "");

        textChannel.sendMessage(new EmbedBuilder()
                .setColor(ColorUtil.PURPLE)
                .setTitle(LanguageUtil.getString(guild, Bundle.STRINGS, "search_ready"))
                .setDescription(LanguageUtil.getLink(guild, url))
                .addField(LanguageUtil.getString(guild, Bundle.CAPTION, "original_link"), String.format("||%s||", url), false)
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
            textChannel.sendMessage(new CommandError(guild, cmd, 1, CommandError.ErrorType.MISSING_VALUE).toEmbed()).queue();
        } else if (args.length == 1) {
            try {
                int maximum = Integer.parseInt(args[0]);
                textChannel.sendMessage(LanguageUtil.getArguedString(guild, Bundle.STRINGS, "random_number", new Random().nextInt(maximum))).queue();
            } catch (NumberFormatException e) {
                if (Long.parseLong(args[0]) > Integer.MAX_VALUE || Long.parseLong(args[0]) < Integer.MIN_VALUE) {
                    textChannel.sendMessage(
                            new CommandError(guild, cmd, 1, CommandError.ErrorType.INCORRECT_RANGE, String.valueOf(Integer.MIN_VALUE), String.valueOf(Integer.MAX_VALUE)).toEmbed()
                    ).queue();
                } else {
                    textChannel.sendMessage(
                            new CommandError(guild, cmd, 0, CommandError.ErrorType.INTEGER).toEmbed()
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
                            new CommandError(guild, cmd, 0, CommandError.ErrorType.INCORRECT_RANGE, String.valueOf(Integer.MIN_VALUE), String.valueOf(Integer.MAX_VALUE)).toEmbed()
                    ).queue();
                } else if (Long.parseLong(args[1]) > Integer.MAX_VALUE || Long.parseLong(args[1]) < Integer.MIN_VALUE) {
                    textChannel.sendMessage(
                            new CommandError(guild, cmd, 1, CommandError.ErrorType.INCORRECT_RANGE, String.valueOf(Integer.MIN_VALUE), String.valueOf(Integer.MAX_VALUE)).toEmbed()
                    ).queue();
                } else {
                    textChannel.sendMessage(
                            new CommandError(guild, cmd, 0, CommandError.ErrorType.INTEGER).toEmbed()
                    ).queue();
                }
            }
        } else {
            cmd.sendUsageEmbed(textChannel);
        }

    }

    @Command(name = "choose",
            category = Command.CommandCategory.FUN,
            executor = Command.CommandExecutor.USER)
    private void choose(Guild guild, TextChannel textChannel, Message message, BotCommand cmd) {

        if (guild == null) return;

        if (message.getMentionedMembers().isEmpty() && message.getMentionedRoles().isEmpty()) {
            textChannel.sendMessage(
                    new CommandError(guild, cmd, 0, CommandError.ErrorType.MISSING_VALUE).toEmbed()
            ).queue();
            return;
        }

        Member chosenOne = null;
        if (!message.getMentionedMembers().isEmpty()) {
            List<Member> members = message.getMentionedMembers();
            chosenOne = members.get(YeahBot.getInstance().getRandomGenerator().nextInt(members.size()));
        } else if (!message.getMentionedRoles().isEmpty()) {
            Role role = message.getMentionedRoles().get(0);
            List<Member> members = guild.getMembers().stream().filter(m -> m.getRoles().contains(role)).collect(Collectors.toList());
            chosenOne = members.get(YeahBot.getInstance().getRandomGenerator().nextInt(members.size()));
        }

        if (chosenOne != null) {
            textChannel.sendMessage(
                    new EmbedBuilder()
                            .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "result"))
                            .setDescription(LanguageUtil.getArguedString(guild, Bundle.STRINGS, "chosen_one",
                                    chosenOne.getAsMention()))
                            .setColor(ColorUtil.PURPLE)
                            .build()
            ).queue();
        } else {
            MessageUtil.getErrorEmbed(guild, LanguageUtil.getString(guild, Bundle.ERROR, "something_went_wrong"));
        }

    }

}
