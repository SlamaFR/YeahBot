package fr.slama.yeahbot.blub;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

/**
 * Created on 01/01/2019.
 */
public final class TaskScheduler implements Runnable {

    private static final Function<String, ThreadFactory> FACTORY = name -> new ThreadFactoryBuilder().setNameFormat("[" + name + "-Pool-%d] ").build();
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool(FACTORY.apply("TaskScheduler"));

    private final Runnable runnable;
    private final boolean repeating;
    private final long period;
    private final long initialDelay;
    private boolean stop = false;

    private TaskScheduler(final Runnable runnable, final long initialDelay, final long period) {
        this.runnable = runnable;
        this.repeating = period > 0;
        this.initialDelay = initialDelay;
        this.period = period;
    }

    private TaskScheduler(final Runnable runnable, final long initialDelay) {
        this(runnable, initialDelay, -1);
    }

    public static TaskScheduler async(final Runnable runnable) {
        return scheduleDelayed(runnable, 0);
    }

    public static TaskScheduler scheduleDelayed(final Runnable runnable, final long initialDelay) {
        TaskScheduler task = new TaskScheduler(runnable, initialDelay);
        EXECUTOR_SERVICE.submit(task);
        return task;
    }

    public static TaskScheduler scheduleRepeating(final Runnable runnable, final long period) {
        return scheduleRepeating(runnable, 0, period);
    }

    public static TaskScheduler scheduleRepeating(final Runnable runnable, final long initialDelay, final long period) {
        TaskScheduler task = new TaskScheduler(runnable, initialDelay, period);
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