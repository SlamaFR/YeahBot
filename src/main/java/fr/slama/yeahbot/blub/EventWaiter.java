package fr.slama.yeahbot.blub;

import fr.slama.yeahbot.YeahBot;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.EventListener;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Created on 17/12/2018.
 */
public class EventWaiter implements EventListener {

    private final Class classType;
    private final Predicate condition;
    private final BiConsumer action;
    private TaskScheduler timeoutTask = null;
    private boolean autoClose;

    private EventWaiter(Builder builder) {
        this.classType = builder.classType;
        this.condition = builder.condition;
        this.autoClose = builder.autoClose;
        this.action = builder.action;

        YeahBot.getInstance().getShardManager().addEventListener(this);

        if (builder.timeout > -1 && builder.unit != null) {
            timeoutTask = TaskScheduler.scheduleDelayed(() -> {
                if (builder.timeoutAction != null) builder.timeoutAction.run();
                this.close();
            }, builder.unit.toMillis(builder.timeout));
        }
    }

    public void close() {
        if (timeoutTask != null) timeoutTask.stop();
        YeahBot.getInstance().getShardManager().removeEventListener(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onEvent(GenericEvent event) {
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
