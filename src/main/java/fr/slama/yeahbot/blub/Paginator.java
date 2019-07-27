package fr.slama.yeahbot.blub;

import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.utilities.EmoteUtil;
import fr.slama.yeahbot.utilities.LanguageUtil;
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
    private boolean ordered;
    private boolean closeable;
    private Consumer<T> selectionResult;
    private Runnable timeoutAction;

    private final int maxPage;
    private int min;
    private int max;

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
        this.ordered = builder.ordered;
        this.closeable = builder.closeable;
        this.selectionResult = builder.selectionResult;
        this.timeoutAction = builder.timeoutAction;

        Checks.notNull(textChannel, "TextChannel");
        Checks.notNull(user, "User");
        Checks.notNull(objectList, "List");
        Checks.notEmpty(objectList, "List");
        Checks.check(pageSize > 0, "Page size must be positive.");
        if (textChannel.getGuild() != null) {
            Checks.check(textChannel.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_ADD_REACTION), "Must have MESSAGE_ADD_REACTION");
            Checks.check(textChannel.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE), "Must have MESSAGE_MANAGE");
        }

        this.maxPage = (int) Math.ceil(this.objectList.size() / (float) this.pageSize) - 1;

        init();
    }

    private void init() {
        this.min = this.pageSize * this.page;
        this.max = this.min + this.pageSize - 1;
        StringBuilder builder = getListBuilder();

        String pageFooter = LanguageUtil.getArguedString(textChannel.getGuild(), Bundle.CAPTION, "page", page + 1, maxPage + 1);
        String expirationFooter = timeout > -1 ? String.format(" • %s", LanguageUtil.getTimeExpiration(textChannel.getGuild(), timeout, unit)) : "";

        EmbedBuilder embed = new EmbedBuilder();

        if (this.embedCustomizer != null) this.embedCustomizer.accept(embed);
        embed.addField(this.listTitle, builder.toString(), false);
        embed.setFooter(pageFooter + expirationFooter, null);

        MessageAction action;
        if (this.message == null) action = this.textChannel.sendMessage(embed.build());
        else action = this.message.editMessage(embed.build());

        List<String> choices = new ArrayList<>(Arrays.asList(EmoteUtil.PREVIOUS, EmoteUtil.NEXT));
        if (this.selectable) choices.addAll(EmoteUtil.getNumbers(this.pageSize));
        if (this.closeable) choices.add(EmoteUtil.NO_REACTION);

        action.queue(msg -> {
            this.message = msg;
            addReactions(msg, choices);

            new EventWaiter.Builder(MessageReactionAddEvent.class,
                    e -> e.getUser().getIdLong() == user.getIdLong() &&
                            (choices.contains(e.getReactionEmote().getName()) || choices.contains(e.getReactionEmote().getId())) &&
                            e.getMessageIdLong() == msg.getIdLong(),
                    (e, ew) -> {
                        e.getReaction().removeReaction(user).queue(SUCCESS, FAILURE);

                        if (e.getReactionEmote().getId() != null &&
                                e.getReactionEmote().getId().equals(EmoteUtil.NO_REACTION)) {
                            msg.delete().queue(SUCCESS, FAILURE);
                            return;
                        }

                        switch (e.getReactionEmote().getName()) {
                            case EmoteUtil.PREVIOUS:
                                if (this.page > 0) this.page--;
                                init();
                                return;
                            case EmoteUtil.NEXT:
                                if (this.page < this.maxPage) this.page++;
                                init();
                                return;
                            default:
                                int index = e.getReactionEmote().getName().charAt(0) - '\u0030' - 1 + min;
                                if (index < this.objectList.size()) {
                                    this.selectionResult.accept(this.objectList.get(index));
                                    msg.delete().queue(SUCCESS, FAILURE);
                                }
                                break;
                        }
                    })
                    .timeout(this.timeout, this.unit)
                    .timeoutAction(() -> {
                        msg.delete().queue(SUCCESS, FAILURE);
                        this.timeoutAction.run();
                    })
                    .build();

        }, FAILURE);
    }

    private StringBuilder getListBuilder() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < this.objectList.size(); i++) {
            T t = this.objectList.get(i);
            if (this.min <= i && i <= this.max) {
                String title = this.objectName.apply(t);
                if (builder.length() > 0) builder.append("\n");
                builder.append(String.format("`%s` - %s", (this.selectable || this.ordered ? i + 1 - min : "●"), title));
            }
        }
        return builder;
    }

    private void addReactions(Message message, List<String> choices) {
        List<String> presentReactions = new ArrayList<>();
        this.message.getReactions().forEach(messageReaction -> presentReactions.add(
                messageReaction.getReactionEmote().getName()
        ));

        for (String choice : choices) {
            if (presentReactions.contains(choice)) continue;
            try {
                message.addReaction(textChannel.getJDA().getEmoteById(choice)).queue(SUCCESS, FAILURE);
            } catch (NumberFormatException e) {
                message.addReaction(choice).queue(SUCCESS, FAILURE);
            }
        }
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
        private boolean ordered = false;
        private boolean closeable = false;
        private Consumer<T> selectionResult;
        private Runnable timeoutAction;

        public Builder(TextChannel textChannel, User user, List<T> objectList) {
            this.textChannel = textChannel;
            this.user = user;
            this.objectList = objectList;
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

        public Builder<T> ordered(boolean ordered) {
            this.ordered = ordered;
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
