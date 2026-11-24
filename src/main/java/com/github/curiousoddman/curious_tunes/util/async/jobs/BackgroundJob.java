package com.github.curiousoddman.curious_tunes.util.async.jobs;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static com.github.curiousoddman.curious_tunes.util.async.jobs.JobStatus.COMPLETED;
import static com.github.curiousoddman.curious_tunes.util.async.jobs.JobStatus.FAILED;
import static com.github.curiousoddman.curious_tunes.util.async.jobs.JobStatus.INTERRUPTED;
import static com.github.curiousoddman.curious_tunes.util.async.jobs.JobStatus.INTERRUPT_REQUESTED;
import static com.github.curiousoddman.curious_tunes.util.async.jobs.JobStatus.NEVER_RUN;
import static com.github.curiousoddman.curious_tunes.util.async.jobs.JobStatus.RUNNING;

/**
 * A long job run by {@link JobManager}, one at a time, with progress reports and cooperative interruption: the job's
 * loop polls {@link #isInterruptRequested()} and returns when it's set.
 * <p>
 * From the async framework of the sibling project (curious_designer), with the changes listed in
 * doc/performance-review.md, item 7.2: progress goes through a {@link JobEventPublisher} (fix 5), and a failed or
 * interrupted job reports it itself (fix 4).
 */
@Slf4j
public abstract class BackgroundJob {
    private static final int PROGRESS_PUBLISH_INTERVAL_MS = 100;

    @Getter
    private volatile JobStatus jobStatus             = NEVER_RUN;
    private volatile long      lastProgressPublishMs = 0;

    protected JobEventPublisher events;

    public abstract String getProcessName();

    /**
     * Whether {@code JobManager} should treat this job class as supersedable: a newer submission of the same class
     * drops any not-yet-started queued instance and requests interruption of a currently-running one, rather than
     * being discarded as a duplicate. Default {@code false}.
     */
    public boolean isSupersedable() {
        return false;
    }

    public void run(JobEventPublisher jobEventPublisher) {
        events = jobEventPublisher;
        if (isInterruptRequested()) {
            log.info("Job interrupted before running");
            jobStatus = INTERRUPTED;
            return;
        }
        try {
            log.info("Set running...");
            jobStatus = RUNNING;
            lastProgressPublishMs = 0;
            runImpl();
            if (jobStatus == INTERRUPT_REQUESTED) {
                log.info("Job interrupted...");
                jobStatus = INTERRUPTED;
                publishInterrupted();
            } else {
                log.info("Job completed");
                jobStatus = COMPLETED;
            }
        } catch (Exception e) {
            log.error("Job failed", e);
            jobStatus = FAILED;
            publishFailed(e);
        }
    }

    public abstract void runImpl() throws Exception;

    public boolean isRunning() {
        return jobStatus == RUNNING;
    }

    public void requestInterrupt() {
        jobStatus = INTERRUPT_REQUESTED;
    }

    /**
     * The job's run loop should poll this periodically (e.g. once per item) and stop promptly once it flips to
     * {@code true}.
     */
    public boolean isInterruptRequested() {
        return jobStatus == INTERRUPT_REQUESTED || jobStatus == INTERRUPTED;
    }

    protected void publishStarted(String description) {
        events.started(this, description);
    }

    /**
     * Unthrottled — use for one-off milestones (e.g. "discovered N files") rather than per-item progress, which
     * should go through {@link #publishProgressThrottled} instead.
     */
    protected void publishInProgress(String description, int progress, int maxProgress) {
        events.progress(this, description, progress, maxProgress);
    }

    /**
     * Throttled per-item progress: only publishes if {@value #PROGRESS_PUBLISH_INTERVAL_MS} ms have passed since the
     * last publish, or {@code isLastItem} is {@code true}. Synchronized, so it's safe to call from several worker
     * threads.
     */
    protected synchronized void publishProgressThrottled(String description, int progress, int maxProgress,
                                                         boolean isLastItem) {
        long nowMs = System.currentTimeMillis();
        if (!isLastItem && nowMs - lastProgressPublishMs < PROGRESS_PUBLISH_INTERVAL_MS) {
            return;
        }
        lastProgressPublishMs = nowMs;
        events.progress(this, description, progress, maxProgress);
    }

    protected void publishEnded(String description) {
        events.ended(this, description);
    }

    private void publishInterrupted() {
        events.interrupted(this);
    }

    private void publishFailed(Throwable e) {
        events.failed(this, e);
    }
}
