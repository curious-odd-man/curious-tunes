package com.github.curiousoddman.curious_tunes.util.async;

import lombok.RequiredArgsConstructor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Runs an action once after a delay; rescheduling before it runs replaces the pending one (debouncing, e.g.
 * saving a setting once a slider stops moving).
 * <p>
 * Thread-safe, and all instances share one daemon scheduler thread (doc/performance-review.md, item 7.2, fix 7),
 * so the actions of all instances run one after another on that thread: keep them short, and hop to the FX thread
 * for UI work.
 * <p>
 * From the async framework of the sibling project (curious_designer).
 */
@RequiredArgsConstructor
public class DelayedAction {
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "delayed-action");
        t.setDaemon(true);
        return t;
    });

    private final long     delay;
    private final TimeUnit timeUnit;

    private ScheduledFuture<?> pending = null;

    public synchronized void reSchedule(Runnable runnable) {
        if (pending != null) {
            pending.cancel(false);
        }
        pending = SCHEDULER.schedule(runnable, delay, timeUnit);
    }
}
