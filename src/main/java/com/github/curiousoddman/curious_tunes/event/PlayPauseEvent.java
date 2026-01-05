package com.github.curiousoddman.curious_tunes.event;

import org.springframework.context.ApplicationEvent;

public class PlayPauseEvent extends ApplicationEvent {
    public PlayPauseEvent(Object source) {
        super(source);
    }
}
