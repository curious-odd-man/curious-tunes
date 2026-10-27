package com.github.curiousoddman.curious_tunes.event;

import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Asks the library window to show the artist and the album of the track and to select the track.
 */
@Getter
public class ShowInLibraryEvent extends ApplicationEvent {
    private final TrackInfo trackInfo;

    public ShowInLibraryEvent(Object source, TrackInfo trackInfo) {
        super(source);
        this.trackInfo = trackInfo;
    }
}
