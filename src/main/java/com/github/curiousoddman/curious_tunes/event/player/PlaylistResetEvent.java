package com.github.curiousoddman.curious_tunes.event.player;

import org.springframework.context.ApplicationEvent;

/**
 * Published when the playlist is fully replaced or cleared (as opposed to tracks being added or moved).
 */
public class PlaylistResetEvent extends ApplicationEvent {
    public PlaylistResetEvent(Object source) {
        super(source);
    }
}
