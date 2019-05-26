package fr.slama.yeahbot.utilities;

import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.language.LanguageUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.requests.restaction.MessageAction;
import net.dv8tion.jda.core.utils.Checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created on 22/12/2018.
 */
public class Paginator<T> {

    private final static Consumer<? super Object> SUCCESS = s -> {
    };
    private final static Consumer<? super Throwable> FAILURE = f -> {
    };

    private TextChannel textChannel;
    private Message message;
    private User user;
    private List<T> objectList;
    private Function<T, String> objectName;
    private Consumer<EmbedBuilder> embedCustomizer;
    private String listTitle;
    private int page = 0;
    private int pageSize;
    private long timeout;
    private TimeUnit unit;
    private boolean selectable;
    private boolean closeable;
    private Consumer<T> selectionResult;
    private Runnable timeoutAction;

    private final int min;
    private final int max;
    private final int maxPage;

    private Paginator(Builder<T> builder) {
        this.textChannel = builder.textChannel;
        this.user = builder.user;
        this.objectList = builder.objectList;
        this.objectName = builder.objectName;
        this.embedCustomizer = builder.embedCustomizer;
        this.listTitle = builder.listTitle;
        this.pageSize = builder.pageSize;
        this.timeout = builder.timeout;
        this.unit = builder.unit;
        this.selectable = builder.selectable;
        this.closeable = builder.closeable;
        this.selectionResult = builder.selectionResult;
        this.timeoutAction = builder.timeoutAction;

        Checks.check(textChannel != null, "TextChannel must not be null.");
        Checks.check(user != null, "User must not be null.");
        Checks.check(objectList != null, "List must not be null.");
        Checks.check(pageSize > 0, "Page size must be positive.");
        if (textChannel.getGuild() != null) {
            Checks.check(textChannel.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_ADD_REACTION), "Must have MESSAGE_ADD_REACTION");
            Checks.check(textChannel.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE), "Must have MESSAGE_MANAGE");
        }

        this.min = this.pageSize * this.page;
        this.max = this.min + this.pageSize - 1;
        this.maxPage = (int) Math.ceil(this.objectList.size() / (float) this.pageSize) - 1;

        init();
    }

    private void init() {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < objectList.size(); i++) {
            T t = objectList.get(i);
            if (min <= i && i <= max) {
                String title = objectName.apply(t);
                if (builder.length() > 0) builder.append("\n");
                builder.append(String.format("`%d` - %s", (selectable ? i + 1 - min : i + 1 - min + page * pageSize), title));
            }
        }

        String pageFooter = LanguageUtil.getArguedString(textChannel.getGuild(), Bundle.CAPTION, "page", page + 1, maxPage + 1);
        String expirationFooter = timeout > -1 ?
                " • " + LanguageUtil.getArguedString(textChannel.getGuild(), Bundle.CAPTION, "custom_time_expiration",
                        timeout,
                        LanguageUtil.getString(textChannel.getGuild(), Bundle.UNIT, unit.toString().toLowerCase() + (timeout > 1 ? "s" : ""))) :
                "";

        EmbedBuilder embed = new EmbedBuilder();

        if (embedCustomizer != null) embedCustomizer.accept(embed);
        embed.addField(listTitle, builder.toString(), false);
        embed.setFooter(pageFooter + expirationFooter, null);

        MessageAction action;
        if (message == null) action = textChannel.sendMessage(embed.build());
        else action = message.editMessage(embed.build());

        List<String> choices = new ArrayList<>(Arrays.asList(EmoteUtil.PREVIOUS, EmoteUtil.NEXT));
        if (selectable) choices.addAll(EmoteUtil.getNumbers(pageSize));
        if (closeable) choices.add(EmoteUtil.NO_EMOTE);

        action.queue(msg -> {
            message = msg;
            for (String choice : choices) {
                try {
                    msg.addReaction(textChannel.getJDA().getEmoteById(choice)).queue(SUCCESS, FAILURE);
                } catch (NumberFormatException e) {
                    msg.addReaction(choice).queue(SUCCESS, FAILURE);
                }
            }

            new EventWaiter(MessageReactionAddEvent.class,
                    e -> e.getUser().getIdLong() == user.getIdLong() &&
                            (choices.contains(e.getReactionEmote().getName()) || choices.contains(e.getReactionEmote().getId())) &&
                            e.getMessageIdLong() == msg.getIdLong(),
                    e -> {
                        e.getReaction().removeReaction(user).queue(SUCCESS, FAILURE);

                        if (e.getReactionEmote().getId() != null &&
                                e.getReactionEmote().getId().equals(EmoteUtil.NO_EMOTE)) {
                            msg.delete().queue(SUCCESS, FAILURE);
                            return;
                        }

                        switch (e.getReactionEmote().getName()) {
                            case EmoteUtil.PREVIOUS:
                                if (page > 0) page--;
                                init();
                                return;
                            case EmoteUtil.NEXT:
                                if (page < maxPage) page++;
                                init();
                                return;
                            default:
                                int index = e.getReactionEmote().getName().charAt(0) - '\u0030' - 1 + min;
                                if (index < objectList.size()) {
                                    selectionResult.accept(objectList.get(index));
                                    msg.delete().queue(SUCCESS, FAILURE);
                                } else init();
                                break;
                        }
                    }, timeout, unit,
                    () -> {
                        msg.delete().queue(SUCCESS, FAILURE);
                        timeoutAction.run();
                    });

        }, FAILURE);
    }

    public static class Builder<T> {
        private TextChannel textChannel;
        private User user;
        private List<T> objectList;
        private Function<T, String> objectName;
        private Consumer<EmbedBuilder> embedCustomizer;
        private String listTitle = EmbedBuilder.ZERO_WIDTH_SPACE;
        private int pageSize = 5;
        private long timeout = -1;
        private TimeUnit unit;
        private boolean selectable = false;
        private boolean closeable = false;
        private Consumer<T> selectionResult;
        private Runnable timeoutAction;

        public Builder() {
        }

        public Builder<T> textChannel(TextChannel textChannel) {
            this.textChannel = textChannel;
            return this;
        }

        public Builder<T> user(User user) {
            this.user = user;
            return this;
        }

        public Builder<T> objectList(List<T> objectList) {
            this.objectList = objectList;
            return this;
        }

        public Builder<T> objectName(Function<T, String> objectName) {
            this.objectName = objectName;
            return this;
        }

        public Builder<T> embedCustomizer(Consumer<EmbedBuilder> embedCustomizer) {
            this.embedCustomizer = embedCustomizer;
            return this;
        }

        public Builder<T> listTitle(String listTitle) {
            this.listTitle = listTitle;
            return this;
        }

        public Builder<T> pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder<T> timeout(long timeout, TimeUnit unit) {
            this.timeout = timeout;
            this.unit = unit;
            return this;
        }

        public Builder<T> selectable(boolean selectable) {
            this.selectable = selectable;
            return this;
        }

        public Builder<T> closeable(boolean closeable) {
            this.closeable = closeable;
            return this;
        }

        public Builder<T> selectionResult(Consumer<T> selectionResult) {
            this.selectionResult = selectionResult;
            return this;
        }

        public Builder<T> timeoutAction(Runnable timeoutAction) {
            this.timeoutAction = timeoutAction;
            return this;
        }

        public Paginator build() {
            return new Paginator<>(this);
        }

    }

}
