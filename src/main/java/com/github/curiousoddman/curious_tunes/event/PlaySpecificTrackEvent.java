package com.github.curiousoddman.curious_tunes.event;

import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;


@Getter
public class PlaySpecificTrackEvent extends ApplicationEvent {
    private final TrackInfo trackToPlay;
    private final boolean addToPlaylistFirst;

    public PlaySpecificTrackEvent(Object source, TrackInfo trackToPlay, boolean addToPlaylistFirst) {
        super(source);
        this.trackToPlay = trackToPlay;
        this.addToPlaylistFirst = addToPlaylistFirst;
    }
}
