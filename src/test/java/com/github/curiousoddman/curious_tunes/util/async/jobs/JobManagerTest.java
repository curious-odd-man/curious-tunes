package com.github.curiousoddman.curious_tunes.util.async.jobs;

import com.github.curiousoddman.curious_tunes.util.async.CancellationToken;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The changes made to the sibling project's async framework (doc/performance-review.md, item 7.2).
 */
class JobManagerTest {

    @Test
    void aCompletedJobReportsItsEnd() throws Exception {
        RecordingEvents events  = new RecordingEvents();
        JobManager      manager = new JobManager(events);
        try {
            manager.submit(new TestJob(() -> {
            }, "done"));
            assertTrue(events.awaitTerminal());
            assertEquals(List.of("started", "ended:done"), events.list);
        } finally {
            manager.close();
        }
    }

    @Test
    void aFailedJobTellsTheUi() throws Exception {          // fix 4
        RecordingEvents events  = new RecordingEvents();
        JobManager      manager = new JobManager(events);
        try {
            manager.submit(new TestJob(() -> {
                throw new IllegalStateException("boom");
            }, null));
            assertTrue(events.awaitTerminal());
            assertEquals(List.of("started", "failed:boom"), events.list);
        } finally {
            manager.close();
        }
    }

    @Test
    void anInterruptedJobTellsTheUi() throws Exception {    // fix 4
        RecordingEvents events  = new RecordingEvents();
        JobManager      manager = new JobManager(events);
        CountDownLatch  running = new CountDownLatch(1);
        TestJob[]       holder  = new TestJob[1];
        holder[0] = new TestJob(() -> {
            running.countDown();
            while (!holder[0].isInterruptRequested()) {
                Thread.onSpinWait();
            }
        }, null);
        try {
            manager.submit(holder[0]);
            assertTrue(running.await(5, TimeUnit.SECONDS));
            manager.interruptCurrentJob();
            assertTrue(events.awaitTerminal());
            assertEquals(List.of("started", "interrupted"), events.list);
        } finally {
            manager.close();
        }
    }

    @Test
    void aJobThatIgnoresInterruptionCannotHangShutdown() throws Exception {     // fix 3
        RecordingEvents events  = new RecordingEvents();
        JobManager      manager = new JobManager(events);
        CountDownLatch  running = new CountDownLatch(1);
        CountDownLatch  release = new CountDownLatch(1);
        manager.submit(new TestJob(() -> {
            running.countDown();
            try {
                release.await();                         // never looks at isInterruptRequested()
            } catch (InterruptedException ignored) {
                // not interrupted by close(): it lets the job stop by itself
            }
        }, null));
        assertTrue(running.await(5, TimeUnit.SECONDS));
        long start = System.nanoTime();
        manager.close();
        long tookMs = (System.nanoTime() - start) / 1_000_000;
        release.countDown();
        assertTrue(tookMs < 8_000, "close() took " + tookMs + " ms");
    }

    @Test
    void aDuplicateJobIsDiscarded() throws Exception {
        RecordingEvents events  = new RecordingEvents();
        JobManager      manager = new JobManager(events);
        CountDownLatch  release = new CountDownLatch(1);
        try {
            assertTrue(manager.submit(new TestJob(() -> {
                try {
                    release.await();
                } catch (InterruptedException ignored) {
                }
            }, "first")).isPresent());
            Thread.sleep(100);
            assertFalse(manager.submit(new TestJob(() -> {
            }, "second")).isPresent());
            release.countDown();
            assertTrue(events.awaitTerminal());
        } finally {
            manager.close();
        }
    }

    @Test
    void aSnapshotThrowsOnceSuperseded() {                   // fix 2
        CancellationToken          token = new CancellationToken();
        CancellationToken.Snapshot first = token.snapshot();
        first.throwIfInvalid();
        token.cancelAndSnapshot();
        assertFalse(first.isValid());
        boolean threw = false;
        try {
            first.throwIfInvalid();
        } catch (CancellationException expected) {
            threw = true;
        }
        assertTrue(threw);
    }

    private static final class TestJob extends BackgroundJob {
        private final Runnable body;
        private final String   endDescription;

        TestJob(Runnable body, String endDescription) {
            this.body = body;
            this.endDescription = endDescription;
        }

        @Override
        public String getProcessName() {
            return "test";
        }

        @Override
        public void runImpl() {
            publishStarted("go");
            body.run();
            if (endDescription != null && !isInterruptRequested()) {
                publishEnded(endDescription);
            }
        }
    }

    private static final class RecordingEvents implements JobEventPublisher {
        final List<String>   list     = new CopyOnWriteArrayList<>();
        final CountDownLatch terminal = new CountDownLatch(1);

        boolean awaitTerminal() throws InterruptedException {
            return terminal.await(5, TimeUnit.SECONDS);
        }

        @Override
        public void started(BackgroundJob job, String description) {
            list.add("started");
        }

        @Override
        public void progress(BackgroundJob job, String description, int progress, int maxProgress) {
            list.add("progress");
        }

        @Override
        public void ended(BackgroundJob job, String description) {
            list.add("ended:" + description);
            terminal.countDown();
        }

        @Override
        public void interrupted(BackgroundJob job) {
            list.add("interrupted");
            terminal.countDown();
        }

        @Override
        public void failed(BackgroundJob job, Throwable error) {
            list.add("failed:" + error.getMessage());
            terminal.countDown();
        }
    }
}
