package com.github.curiousoddman.curious_tunes.util.async.jobs;

/**
 * Where a {@link BackgroundJob} reports its progress. Each application implements it with its own events, so the
 * jobs framework doesn't depend on any one application's event classes (doc/performance-review.md, item 7.2, fix 5).
 * Called from the job's worker thread.
 */
public interface JobEventPublisher {
    void started(BackgroundJob job, String description);

    /**
     * @param maxProgress the total; {@code progress} counts up to it
     */
    void progress(BackgroundJob job, String description, int progress, int maxProgress);

    void ended(BackgroundJob job, String description);

    void interrupted(BackgroundJob job);

    void failed(BackgroundJob job, Throwable error);
}
