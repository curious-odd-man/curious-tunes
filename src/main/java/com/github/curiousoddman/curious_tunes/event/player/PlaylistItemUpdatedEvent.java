package com.github.curiousoddman.curious_tunes.event.player;

import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistItem;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * The state of one queue item changed outside of playback (for example its file was found to be missing),
 * so its row has to be styled again.
 */
@Getter
public class PlaylistItemUpdatedEvent extends ApplicationEvent {
    private final PlaylistItem playlistItem;

    public PlaylistItemUpdatedEvent(Object source, PlaylistItem playlistItem) {
        super(source);
        this.playlistItem = playlistItem;
    }
}
