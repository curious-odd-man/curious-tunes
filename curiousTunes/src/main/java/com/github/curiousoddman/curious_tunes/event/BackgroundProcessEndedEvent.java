package com.github.curiousoddman.curious_tunes.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class BackgroundProcessEndedEvent extends ApplicationEvent {
    private final String description;
    private final Exception error;

    public BackgroundProcessEndedEvent(Object source, String description, Exception error) {
        super(source);
        this.description = description;
        this.error = error;
    }
}