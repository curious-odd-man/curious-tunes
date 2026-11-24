package com.github.curiousoddman.curious_tunes.util.async.jobs;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Runs {@link BackgroundJob}s one at a time on a single worker thread, from a queue. A job of the same class as one
 * queued or running is discarded as a duplicate, unless the job is supersedable
 * ({@link BackgroundJob#isSupersedable()}).
 * <p>
 * From the async framework of the sibling project (curious_designer), with the changes listed in
 * doc/performance-review.md, item 7.2: events go through a {@link JobEventPublisher} (fix 5), the worker is a daemon
 * thread, and shutdown waits for the current job only for a limited time (fix 3).
 */
@Slf4j
@Component
public class JobManager {
    /**
     * How long shutdown waits for the current job to notice its interruption.
     */
    private static final long SHUTDOWN_WAIT_MS = 5_000;

    private final LinkedBlockingQueue<ManagedJob> queue = new LinkedBlockingQueue<>();
    private final JobEventPublisher               events;
    private final Thread                          worker;

    private volatile ManagedJob currentJob;
    private volatile boolean    shutdown;

    public JobManager(JobEventPublisher events) {
        this.events = events;
        worker = Thread.ofPlatform()
                       .name("BackgroundJob")
                       .daemon(true)               // a stuck job can never keep the JVM alive
                       .start(this::workerLoop);
    }

    /**
     * @return the job's descriptor, or empty if it was discarded (a duplicate, or shutting down)
     */
    public synchronized Optional<JobDescriptor> submit(BackgroundJob job) {
        if (shutdown) {
            return Optional.empty();
        }
        if (job.isSupersedable()) {
            return submitSupersedable(job);
        }

        var queuedClasses = queue.stream()
                                 .map(ManagedJob::job)
                                 .map(Object::getClass)
                                 .toList();

        if (currentJob != null && sameClass(job, currentJob.job())
                || queuedClasses.contains(job.getClass())) {
            log.info("Discarded job submission - same class '{}' already in queue '{}'", job.getClass(), queuedClasses);
            return Optional.empty();
        }

        return enqueue(job);
    }

    /**
     * Supersedable jobs always get accepted: any not-yet-started queued instance of the same class is dropped, and a
     * currently-running instance of the same class is asked to stop via {@link BackgroundJob#requestInterrupt()}.
     */
    private Optional<JobDescriptor> submitSupersedable(BackgroundJob job) {
        queue.removeIf(managed -> sameClass(job, managed.job()));
        if (currentJob != null && sameClass(job, currentJob.job())) {
            currentJob.job()
                      .requestInterrupt();
        }
        return enqueue(job);
    }

    private Optional<JobDescriptor> enqueue(BackgroundJob job) {
        JobDescriptor descriptor = new JobDescriptor(UUID.randomUUID(), job.getProcessName(), "");
        ManagedJob    managed    = new ManagedJob(job, descriptor);

        queue.add(managed);
        log.info("Added job to queue. Current length = {}", queue.size());
        return Optional.of(descriptor);
    }

    public List<JobDescriptor> getQueuedJobs() {
        return queue.stream()
                    .map(ManagedJob::descriptor)
                    .toList();
    }

    public Optional<JobDescriptor> currentJob() {
        return Optional.ofNullable(currentJob)
                       .map(ManagedJob::descriptor);
    }

    public void interruptCurrentJob() {
        ManagedJob current = currentJob;
        log.info("Requested interruption of current job: {}", current);
        if (current != null) {
            cancel(current.descriptor()
                          .getId());
        }
    }

    public void cancel(UUID id) {
        log.info("Requested interrupt/cancel of job {}", id);
        ManagedJob current = currentJob;
        if (current != null && current.descriptor()
                                      .getId()
                                      .equals(id)) {
            current.job()
                   .requestInterrupt();
            log.info("Interrupting current job...");
            return;
        }

        for (ManagedJob managedJob : queue) {
            if (managedJob.descriptor()
                          .getId()
                          .equals(id)) {
                managedJob.job()
                          .requestInterrupt();
                log.info("Cancelling queued job");
                return;
            }
        }
    }

    public void cancelAll() {
        log.info("Cancel all requested...");
        ManagedJob queued;

        while ((queued = queue.poll()) != null) {
            queued.job()
                  .requestInterrupt();
        }

        ManagedJob current = currentJob;
        if (current != null) {
            current.job()
                   .requestInterrupt();
        }
    }

    private void workerLoop() {
        while (!shutdown) {
            ManagedJob managed;

            try {
                do {
                    managed = queue.take();
                    log.info("Got job '{}' in status '{}'",
                            managed.descriptor()
                                   .getName(),
                            managed.job()
                                   .getJobStatus());
                } while (managed.job()
                                .isInterruptRequested());
            } catch (InterruptedException e) {
                if (shutdown) {
                    break;
                }

                continue;
            }

            synchronized (this) {
                currentJob = managed;
            }

            try {
                managed.job()
                       .run(events);
            } catch (Throwable t) {
                log.error("Background Job failed {}", t.getMessage(), t);
            } finally {
                synchronized (this) {
                    currentJob = null;
                }
            }
        }
    }

    /**
     * Interrupts all jobs and waits up to {@value #SHUTDOWN_WAIT_MS} ms for the current one to stop; a job that
     * doesn't poll {@link BackgroundJob#isInterruptRequested()} can no longer hang the shutdown.
     */
    @PreDestroy
    public void close() {
        log.info("Shutting down...");
        cancelAll();
        shutdown = true;
        // Let the current job stop by itself first: interrupting its thread could break its file I/O midway.
        long deadline = System.currentTimeMillis() + SHUTDOWN_WAIT_MS;
        try {
            while (currentJob != null && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread()
                  .interrupt();
        }
        if (currentJob != null) {
            log.warn("Background job '{}' did not stop within {} ms; leaving it", currentJob(), SHUTDOWN_WAIT_MS);
            return;                     // the worker is a daemon thread: it won't keep the JVM alive
        }
        worker.interrupt();             // wakes the worker waiting for a job, so it ends
    }

    private static boolean sameClass(BackgroundJob job, BackgroundJob other) {
        return other.getClass()
                    .equals(job.getClass());
    }
}
