package fr.slama.yeahbot.listeners;

import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.utilities.EmoteUtil;
import fr.slama.yeahbot.utilities.MessageUtil;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.io.Closeable;
import java.util.*;
import java.util.function.Consumer;

/**
 * Created on 24/09/2018.
 */

public class SelectionListener extends ListenerAdapter implements Closeable {

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
    private static final Consumer<? super Void> SUCCESS = s -> {
    };
    private static final Consumer<Throwable> FAILURE = s -> {
    };

    private final User user;
    private final Message message;
    private final ArrayList<String> choices;
    private final Consumer<List<String>> result;
    private final boolean multiple;
    private final Timer timer = new Timer();
    private final List<String> selection = new ArrayList<>();

    public SelectionListener(Message message, User user, int delay, ArrayList<String> choices, Consumer<List<String>> result, boolean multiple) {
        this.user = user;
        this.message = message;
        this.choices = choices;
        this.result = result;
        this.multiple = multiple;

        if (!message.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) {
            MessageUtil.sendPermissionEmbed(message.getGuild(), message.getTextChannel(), Permission.MESSAGE_MANAGE);
            return;
        }

        if (!message.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_ADD_REACTION)) {
            MessageUtil.sendPermissionEmbed(message.getGuild(), message.getTextChannel(), Permission.MESSAGE_ADD_REACTION);
            message.delete().queue();
            return;
        }

        YeahBot.getInstance().getShardManager().addEventListener(this);

        if (delay > -1) {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    message.delete().queue();
                    YeahBot.getInstance().getShardManager().removeEventListener(this);
                }
            }, delay);
        }

        if (!message.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_HISTORY)) {
            MessageUtil.sendPermissionEmbed(message.getGuild(), message.getTextChannel(), Permission.MESSAGE_HISTORY);
            message.delete().queue();
            return;
        }

        for (String emote : choices) {
            try {
                long l = Long.parseLong(emote);
                message.addReaction(YeahBot.getInstance().getShardManager().getEmoteById(l)).queue(SUCCESS, FAILURE);
            } catch (NumberFormatException e) {
                message.addReaction(emote).queue(SUCCESS, FAILURE);
            }
        }

        if (multiple) message.addReaction(OK_EMOTE).queue();
    }

    public static ArrayList<String> get(int count) {
        return new ArrayList<>(Arrays.asList(EMOTES).subList(0, count));
    }

    public static ArrayList<String> getQuestion() {
        return new ArrayList<>(Arrays.asList(EmoteUtil.NO_EMOTE, EmoteUtil.YES_EMOTE));
    }

    @Override
    public void close() {
        YeahBot.getInstance().getShardManager().removeEventListener(this);
        result.accept(selection);
        message.delete().queue();
        timer.cancel();
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        super.onMessageReactionAdd(event);
        handleEvent(event, () -> {
            String s = event.getReaction().getReactionEmote().getName();
            String id = event.getReaction().getReactionEmote().getId();

            if (choices.contains(s) && !selection.contains(s)) selection.add(s);
            if (choices.contains(id) && !selection.contains(id)) selection.add(id);

            if (multiple) {
                if (OK_EMOTE.equals(s)) close();
            } else {
                close();
            }
        });
    }

    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
        super.onMessageReactionRemove(event);
        handleEvent(event, () -> {
            String s = event.getReaction().getReactionEmote().getName();
            String id = event.getReaction().getReactionEmote().getId();

            selection.remove(s);
            selection.remove(id);
        });
    }

    private void handleEvent(GenericMessageReactionEvent event, Runnable runnable) {
        if (event.getMessageId().equals(message.getId())) {
            event.getChannel().getMessageById(event.getMessageIdLong()).queue((Message m) -> event.getReaction().getUsers().queue(users -> {
                if (users.contains(event.getJDA().getSelfUser()) && event.getUser().equals(user)) runnable.run();
            }));
        }
    }
}