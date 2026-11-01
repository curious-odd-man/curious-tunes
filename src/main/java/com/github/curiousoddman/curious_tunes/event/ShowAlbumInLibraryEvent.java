package com.github.curiousoddman.curious_tunes.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Asks the library window to show an artist and, if given, to scroll to one of the albums.
 */
@Getter
public class ShowAlbumInLibraryEvent extends ApplicationEvent {
    private final int     artistId;
    private final Integer albumId;

    public ShowAlbumInLibraryEvent(Object source, int artistId, Integer albumId) {
        super(source);
        this.artistId = artistId;
        this.albumId = albumId;
    }
}
