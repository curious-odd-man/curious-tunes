package com.github.curiousoddman.curious_tunes.domain.jobs;

import com.github.curiousoddman.curious_tunes.event.BackgroundProcessEvent;
import com.github.curiousoddman.curious_tunes.event.types.BackgroundProcessEventType;
import com.github.curiousoddman.curious_tunes.util.async.jobs.BackgroundJob;
import com.github.curiousoddman.curious_tunes.util.async.jobs.JobEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Turns the progress of {@link BackgroundJob}s into this app's {@link BackgroundProcessEvent}s, which the status
 * area of the library window shows.
 */
@Component
@RequiredArgsConstructor
public class BackgroundProcessJobEvents implements JobEventPublisher {
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void started(BackgroundJob job, String description) {
        publish(builder(job, BackgroundProcessEventType.STARTED).description(description)
                                                                .maxProgress(-1));
    }

    @Override
    public void progress(BackgroundJob job, String description, int progress, int maxProgress) {
        publish(builder(job, BackgroundProcessEventType.IN_PROGRESS).description(description)
                                                                    .progress(progress)
                                                                    .maxProgress(maxProgress));
    }

    @Override
    public void ended(BackgroundJob job, String description) {
        publish(builder(job, BackgroundProcessEventType.ENDED).description(description));
    }

    @Override
    public void interrupted(BackgroundJob job) {
        publish(builder(job, BackgroundProcessEventType.INTERRUPTED).description("Interrupted"));
    }

    @Override
    public void failed(BackgroundJob job, Throwable error) {
        publish(builder(job, BackgroundProcessEventType.FAILED).description("Failed...")
                                                               .error(error instanceof Exception e
                                                                       ? e
                                                                       : new RuntimeException(error)));
    }

    private static BackgroundProcessEvent.BackgroundProcessEventBuilder builder(BackgroundJob job,
                                                                                BackgroundProcessEventType type) {
        return BackgroundProcessEvent.builder()
                                     .source(job)
                                     .processName(job.getProcessName())
                                     .eventType(type);
    }

    private void publish(BackgroundProcessEvent.BackgroundProcessEventBuilder builder) {
        eventPublisher.publishEvent(builder.build());
    }
}
