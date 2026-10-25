package com.github.curiousoddman.curious_tunes.domain.player;

import java.util.OptionalLong;
import java.util.function.LongSupplier;

/**
 * Wall-clock stopwatch for the listening session.
 * <p>
 * It is not started until {@link #startIfNotStarted()} is called, keeps running through pauses and seeks,
 * and goes back to the "not started" state on {@link #reset()}.
 */
public class SessionClock {
    private final LongSupplier nanoTime;
    private long startNanos;
    private boolean started;

    public SessionClock() {
        this(System::nanoTime);
    }

    public SessionClock(LongSupplier nanoTime) {
        this.nanoTime = nanoTime;
    }

    public synchronized void startIfNotStarted() {
        if (!started) {
            startNanos = nanoTime.getAsLong();
            started = true;
        }
    }

    public synchronized void reset() {
        started = false;
    }

    public synchronized boolean isStarted() {
        return started;
    }

    /**
     * @return seconds since the session started, or empty if it has not started
     */
    public synchronized OptionalLong elapsedSeconds() {
        if (!started) {
            return OptionalLong.empty();
        }
        long elapsedNanos = nanoTime.getAsLong() - startNanos;
        return OptionalLong.of(Math.max(0, elapsedNanos / 1_000_000_000L));
    }

    /**
     * Formats as {@code m:ss}, or {@code h:mm:ss} once an hour has passed.
     */
    public static String format(long totalSeconds) {
        long seconds = Math.max(0, totalSeconds);
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) {
            return "%d:%02d:%02d".formatted(h, m, s);
        }
        return "%d:%02d".formatted(m, s);
    }
}
