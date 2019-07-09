package fr.slama.yeahbot.blub;

import fr.slama.yeahbot.YeahBot;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.hooks.EventListener;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Created on 17/12/2018.
 */
public class EventWaiter implements EventListener, Closeable {

    private final Class classType;
    private final Predicate condition;
    private final BiConsumer action;
    private boolean autoClose;

    private EventWaiter(Builder builder) {
        this.classType = builder.classType;
        this.condition = builder.condition;
        this.autoClose = builder.autoClose;
        this.action = builder.action;

        YeahBot.getInstance().getShardManager().addEventListener(this);

        Runnable runnable = () -> {
            if (builder.timeoutAction != null) builder.timeoutAction.run();
            this.close();
        };

        if (builder.timeout > 0 && builder.unit != null) {
            TaskScheduler.scheduleDelayed(runnable, builder.unit.toMillis(builder.timeout));
        }
    }

    @Override
    public void close() {
        YeahBot.getInstance().getShardManager().removeEventListener(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onEvent(Event event) {
        if (event.getClass().equals(classType) && (condition.test(event))) {
            action.accept(event, this);
            if (this.autoClose) this.close();
        }
    }

    public static class Builder {

        private final Class classType;
        private final Predicate condition;
        private final BiConsumer action;
        private Runnable timeoutAction;
        private boolean autoClose = true;
        private long timeout = -1;
        private TimeUnit unit;

        public <T extends Event> Builder(Class<T> classType, Predicate<T> condition, BiConsumer<T, EventWaiter> action) {
            this.classType = classType;
            this.condition = condition;
            this.action = action;
        }

        public Builder autoClose(boolean autoClose) {
            this.autoClose = autoClose;
            return this;
        }

        public Builder timeout(long timeout, TimeUnit unit) {
            this.timeout = timeout;
            this.unit = unit;
            return this;
        }

        public Builder timeoutAction(Runnable timeoutAction) {
            this.timeoutAction = timeoutAction;
            return this;
        }

        public EventWaiter build() {
            return new EventWaiter(this);
        }
    }

}
