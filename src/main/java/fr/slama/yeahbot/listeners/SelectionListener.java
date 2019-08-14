package fr.slama.yeahbot.listeners;

import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.blub.TaskScheduler;
import fr.slama.yeahbot.utilities.EmoteUtil;
import fr.slama.yeahbot.utilities.MessageUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created on 24/09/2018.
 */

public class SelectionListener extends ListenerAdapter {

    private static final String[] EMOTES = new String[]{
            "\u0031\u20E3",
            "\u0032\u20E3",
            "\u0033\u20E3",
            "\u0034\u20E3",
            "\u0035\u20E3",
            "\u0036\u20E3",
            "\u0037\u20E3",
            "\u0038\u20E3",
            "\u0039\u20E3",
            "\uD83D\uDD1F"
    };
    private static final String OK_EMOTE = "✅";

    private final User user;
    private final Message message;
    private final List<String> choices;
    private final Consumer<List<String>> result;
    private final boolean multiple;
    private TaskScheduler task = null;
    private final List<String> selection = new ArrayList<>();

    public SelectionListener(Message message, User user, int delay, List<String> choices, Consumer<List<String>> result, boolean multiple) {
        this.user = user;
        this.message = message;
        this.choices = choices;
        this.result = result;
        this.multiple = multiple;

        if (!message.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_ADD_REACTION)) {
            MessageUtil.sendPermissionEmbed(message.getGuild(), message.getTextChannel(), Permission.MESSAGE_ADD_REACTION);
            close();
            return;
        }
        YeahBot.getInstance().getShardManager().addEventListener(this);

        if (delay > -1) task = TaskScheduler.scheduleDelayed(this::close, delay);

        if (!message.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_HISTORY)) {
            MessageUtil.sendPermissionEmbed(message.getGuild(), message.getTextChannel(), Permission.MESSAGE_HISTORY);
            close();
            return;
        }

        for (String emote : choices) {
            try {
                long l = Long.parseLong(emote);
                message.addReaction(YeahBot.getInstance().getShardManager().getEmoteById(l)).queue();
            } catch (NumberFormatException e) {
                message.addReaction(emote).queue();
            }
        }
        if (multiple) message.addReaction(OK_EMOTE).queue();
    }

    public static List<String> get(int count) {
        return new ArrayList<>(Arrays.asList(EMOTES).subList(0, count));
    }

    public static List<String> getQuestion() {
        return EmoteUtil.getQuestionEmotes();
    }

    public void close() {
        YeahBot.getInstance().getShardManager().removeEventListener(this);
        result.accept(selection);
        message.delete().queue();
        if (task != null) task.stop();
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        super.onMessageReactionAdd(event);
        handleEvent(event, () -> {

            String emote = event.getReactionEmote().isEmoji() ? event.getReactionEmote().getName() : event.getReactionEmote().getId();
            if (choices.contains(emote) && !selection.contains(emote)) selection.add(emote);

            if (multiple) {
                if (OK_EMOTE.equals(emote)) close();
            } else {
                close();
            }
        });
    }

    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
        super.onMessageReactionRemove(event);
        handleEvent(event, () -> {
            String emote = event.getReactionEmote().isEmoji() ? event.getReactionEmote().getName() : event.getReactionEmote().getId();
            selection.remove(emote);
        });
    }

    private void handleEvent(GenericMessageReactionEvent event, Runnable runnable) {
        if (event.getMessageId().equals(message.getId())) {
            event.getChannel().retrieveMessageById(event.getMessageIdLong()).queue((Message m) -> event.getReaction().retrieveUsers().queue(users -> {
                if (users.contains(event.getJDA().getSelfUser()) && event.getUser().getIdLong() == user.getIdLong())
                    runnable.run();
            }));
        }
    }
}