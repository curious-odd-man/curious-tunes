package com.github.curiousoddman.curious_tunes.event;

import org.springframework.context.ApplicationEvent;

/**
 * Request to skip the currently playing track and continue with the next one in the playlist.
 */
public class SkipNextEvent extends ApplicationEvent {
    public SkipNextEvent(Object source) {
        super(source);
    }
}
