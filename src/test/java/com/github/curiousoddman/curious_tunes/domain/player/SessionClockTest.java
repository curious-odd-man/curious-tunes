package com.github.curiousoddman.curious_tunes.domain.player;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionClockTest {
    private static final long SECOND = 1_000_000_000L;

    private final AtomicLong now = new AtomicLong(5 * SECOND);
    private final SessionClock clock = new SessionClock(now::get);

    @Test
    void emptyUntilStarted() {
        assertFalse(clock.isStarted());
        assertTrue(clock.elapsedSeconds().isEmpty());
    }

    @Test
    void countsWallClockTimeFromFirstStart() {
        clock.startIfNotStarted();
        now.addAndGet(5 * SECOND + SECOND / 2);
        assertEquals(5, clock.elapsedSeconds().getAsLong());
    }

    @Test
    void secondStartDoesNotRestartTheSession() {
        clock.startIfNotStarted();
        now.addAndGet(10 * SECOND);
        clock.startIfNotStarted();
        now.addAndGet(5 * SECOND);
        assertEquals(15, clock.elapsedSeconds().getAsLong());
    }

    @Test
    void resetClearsAndNextStartBeginsFromZero() {
        clock.startIfNotStarted();
        now.addAndGet(30 * SECOND);
        clock.reset();
        assertTrue(clock.elapsedSeconds().isEmpty());

        now.addAndGet(100 * SECOND);
        clock.startIfNotStarted();
        assertEquals(0, clock.elapsedSeconds().getAsLong());
    }

    @Test
    void formatsMinutesAndSecondsThenHours() {
        assertEquals("0:00", SessionClock.format(0));
        assertEquals("0:07", SessionClock.format(7));
        assertEquals("1:05", SessionClock.format(65));
        assertEquals("59:59", SessionClock.format(3599));
        assertEquals("1:00:00", SessionClock.format(3600));
        assertEquals("1:02:05", SessionClock.format(3725));
        assertEquals("0:00", SessionClock.format(-3));
    }
}
