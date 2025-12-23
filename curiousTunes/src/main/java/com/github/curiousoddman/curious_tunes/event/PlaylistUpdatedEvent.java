package com.github.curiousoddman.curious_tunes.event;

import org.springframework.context.ApplicationEvent;

public class PlaylistUpdatedEvent extends ApplicationEvent {
    public PlaylistUpdatedEvent(Object source) {
        super(source);
    }
}
