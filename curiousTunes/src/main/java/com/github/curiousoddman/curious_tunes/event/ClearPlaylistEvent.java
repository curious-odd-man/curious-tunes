package com.github.curiousoddman.curious_tunes.event;

import org.springframework.context.ApplicationEvent;

public class ClearPlaylistEvent extends ApplicationEvent {
    public ClearPlaylistEvent(Object source) {
        super(source);
    }
}
