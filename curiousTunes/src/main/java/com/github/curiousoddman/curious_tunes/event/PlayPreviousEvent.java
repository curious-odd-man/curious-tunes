package com.github.curiousoddman.curious_tunes.event;

import org.springframework.context.ApplicationEvent;

public class PlayPreviousEvent extends ApplicationEvent {
    public PlayPreviousEvent(Object source) {
        super(source);
    }
}
