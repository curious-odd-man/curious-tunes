package com.github.curiousoddman.curious_tunes.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class BackgroundProcessEvent extends ApplicationEvent {
    private final String description;
    private final int progress;
    private final int maxProgress;

    public BackgroundProcessEvent(Object source, String description, int progress, int maxProgress) {
        super(source);
        this.description = description;
        this.progress = progress;
        this.maxProgress = maxProgress;
    }
}