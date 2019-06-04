package fr.slama.yeahbot.utilities;

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

    private final EventWaiter INSTANCE;

    public <T extends Event, U extends EventWaiter> EventWaiter(Class<T> classType, Predicate<T> condition, BiConsumer<T, U> action) {
        this(classType, condition, action, true);
    }

    public <T extends Event, U extends EventWaiter> EventWaiter(Class<T> classType, Predicate<T> condition, BiConsumer<T, U> action, boolean autoClose) {
        this(classType, condition, action, -1, null, null);
        this.autoClose = autoClose;
    }

    public <T extends Event, U extends EventWaiter> EventWaiter(Class<T> classType, Predicate<T> condition, BiConsumer<T, U> action,
                                                                long timeout, TimeUnit unit, Runnable timeoutAction) {

        this.classType = classType;
        this.condition = condition;
        this.autoClose = true;
        this.action = action;
        this.INSTANCE = this;

        YeahBot.getInstance().getShardManager().addEventListener(INSTANCE);

        Runnable runnable = () -> {
            if (timeoutAction != null) timeoutAction.run();
            INSTANCE.close();
        };

        if (timeout > 0 && unit != null) {
            TaskScheduler.async(runnable, unit.toMillis(timeout));
        }
    }

    @Override
    public void close() {
        YeahBot.getInstance().getShardManager().removeEventListener(INSTANCE);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void onEvent(Event event) {
        if (event.getClass().equals(classType) && (condition.test(event))) {
            action.accept(event, this);
            if (this.autoClose) INSTANCE.close();
        }
    }

}
