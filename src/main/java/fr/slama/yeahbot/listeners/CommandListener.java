package fr.slama.yeahbot.listeners;

import fr.slama.yeahbot.commands.core.CommandMap;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.language.LanguageUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 * Created on 22/09/2018.
 */
public class CommandListener extends ListenerAdapter {

    private final CommandMap commandMap;

    public CommandListener(CommandMap commandMap) {
        this.commandMap = commandMap;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        super.onMessageReceived(event);
        if (event.getAuthor().equals(event.getJDA().getSelfUser())) return;

        String message = event.getMessage().getContentRaw();

        if (message.startsWith(CommandMap.getPrefix(event.getGuild()))) {
            String[] args = message.split(" ");
            args[0] = args[0].replace(CommandMap.getPrefix(event.getGuild()), "");
            commandMap.commandUser(event.getAuthor(), String.join(" ", args), event.getMessage());
        } else if (event.getMessage().getMentionedUsers().contains(event.getJDA().getSelfUser()))
            try {
                event.getChannel().sendMessage(
                        new EmbedBuilder()
                                .setTitle(LanguageUtil.getString(event.getGuild(), Bundle.CAPTION, "information"))
                                .setDescription(LanguageUtil.getArguedString(event.getGuild(), Bundle.STRINGS, "current_prefix", CommandMap.getPrefix(event.getGuild())))
                                .build()
                ).queue();
            } catch (InsufficientPermissionException ignored) {
            }
    }
}
