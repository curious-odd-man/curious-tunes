package com.github.curiousoddman.curious_tunes.event.player;

import com.github.curiousoddman.curious_tunes.model.PlaybackState;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistItem;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PlayerStatusEvent extends ApplicationEvent {
    private final PlaybackState status;
    private final PlaylistItem playlistItem;

    public PlayerStatusEvent(Object source, PlaybackState status, PlaylistItem playlistItem) {
        super(source);
        this.status = status;
        this.playlistItem = playlistItem;
    }
}
