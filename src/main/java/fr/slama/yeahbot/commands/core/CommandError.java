package fr.slama.yeahbot.commands.core;

import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.utilities.ColorUtil;
import fr.slama.yeahbot.utilities.LanguageUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;

/**
 * Created on 13/11/2018.
 */
public class CommandError {

    private final String argument;
    private final Guild guild;
    private final ErrorType type;
    private final String[] args;
    private BotCommand cmd;

    public CommandError(BotCommand cmd, String argument, Guild guild, ErrorType type, String... args) {
        this.cmd = cmd;
        this.argument = argument;
        this.guild = guild;
        this.type = type;
        this.args = args;
    }

    public MessageEmbed toEmbed() {
        String usageCaption = LanguageUtil.getString(guild, Bundle.CAPTION, "usage");
        return new EmbedBuilder()
                .setColor(ColorUtil.RED)
                .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "argument_error"))
                .setDescription(LanguageUtil.getArguedString(guild, Bundle.ERROR, type.toString(), argument, args))
                .addField(usageCaption, cmd.getUsage(guild), false)
                .build();
    }

    public enum ErrorType {

        ENTITY_NOT_FOUND,
        INCORRECT_VALUE,
        INCORRECT_RANGE,
        INTEGER,
        MISSING_VALUE,
        TOO_LONG,
        URL;

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

}
