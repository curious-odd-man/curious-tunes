package com.github.curiousoddman.curious_tunes.event.player;

import org.springframework.context.ApplicationEvent;

/**
 * Published after the order or the content of the queue changed (tracks added, removed, moved or shuffled).
 * The player uses it to preload the right next track.
 */
public class PlaylistChangedEvent extends ApplicationEvent {
    public PlaylistChangedEvent(Object source) {
        super(source);
    }
}
