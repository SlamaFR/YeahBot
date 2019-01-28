package fr.slama.yeahbot.utilities;

import fr.slama.yeahbot.YeahBot;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.hooks.EventListener;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created on 17/12/2018.
 */
public class EventWaiter implements EventListener, Closeable {

    private final Class classType;
    private final Predicate condition;
    private final Consumer action;

    private final EventWaiter INSTANCE;
    private Runnable future;

    public <T extends Event> EventWaiter(Class<T> classType, Predicate<T> condition, Consumer<T> action) {
        this(classType, condition, action, -1, null, null);
    }

    public <T extends Event> EventWaiter(Class<T> classType, Predicate<T> condition, Consumer<T> action,
                                         long timeout, TimeUnit unit, Runnable timeoutAction) {

        this.classType = classType;
        this.condition = condition;
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
        if (event.getClass().equals(classType)) {
            if (condition.test(event)) {
                action.accept(event);
                INSTANCE.close();
            }
        }
    }

}
