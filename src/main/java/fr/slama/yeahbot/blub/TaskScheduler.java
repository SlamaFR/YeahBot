package fr.slama.yeahbot.blub;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Created on 01/01/2019.
 */
public final class TaskScheduler implements Runnable {

    private static final Function<String, ThreadFactory> FACTORY = name -> new ThreadFactoryBuilder().setNameFormat("[" + name + "-Pool-%d] ").build();
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool(FACTORY.apply("TaskScheduler"));

    private final Runnable runnable;
    private final boolean repeating;
    private final long period;
    private final Predicate<LocalDate> predicate;
    private final long initialDelay;
    private boolean stop = false;

    private TaskScheduler(final Runnable runnable, final long initialDelay, final long period, final Predicate<LocalDate> predicate) {
        this.runnable = runnable;
        this.repeating = true;
        this.initialDelay = initialDelay;
        this.period = period;
        this.predicate = predicate;
    }

    private TaskScheduler(final Runnable runnable, final long initialDelay, final long period) {
        this(runnable, initialDelay, period, null);
    }

    private TaskScheduler(final Runnable runnable, final long initialDelay) {
        this.initialDelay = initialDelay;
        this.runnable = runnable;
        this.repeating = false;
        this.period = -1;
        this.predicate = null;
    }

    private TaskScheduler(final Runnable runnable) {
        this.initialDelay = -1;
        this.runnable = runnable;
        this.repeating = false;
        this.period = -1;
        this.predicate = null;
    }

    public static TaskScheduler scheduleDelayed(final Runnable runnable, final long initialDelay) {
        TaskScheduler task = new TaskScheduler(runnable, initialDelay);
        EXECUTOR_SERVICE.submit(task);
        return task;
    }

    public static TaskScheduler scheduleRepeating(final Runnable runnable, final long period) {
        TaskScheduler task = new TaskScheduler(runnable, 0, period);
        EXECUTOR_SERVICE.submit(task);
        return task;
    }

    public static TaskScheduler scheduleRepeating(final Runnable runnable, final long initialDelay, final long period) {
        TaskScheduler task = new TaskScheduler(runnable, initialDelay, period);
        EXECUTOR_SERVICE.submit(task);
        return task;
    }

    public static TaskScheduler async(final Runnable runnable) {
        TaskScheduler task = new TaskScheduler(runnable);
        EXECUTOR_SERVICE.submit(task);
        return task;
    }

    public void stop() {
        stop = true;
    }

    @Override
    public void run() {
        initWait();
        if (!this.repeating && !stop)
            runnable.run();

        while (repeating && !stop) {
            if (predicate != null && !predicate.test(LocalDate.now()))
                waitNow(period);
            runnable.run();
            waitNow(period);
        }
    }

    private void initWait() {
        if (initialDelay > 0) waitNow(initialDelay);
    }

    private void waitNow(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}