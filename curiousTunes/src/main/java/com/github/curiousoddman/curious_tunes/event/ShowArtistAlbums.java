package com.github.curiousoddman.curious_tunes.event;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ShowArtistAlbums extends ApplicationEvent {
    private final ArtistRecord artistRecord;

    public ShowArtistAlbums(Object source, ArtistRecord artistRecord) {
        super(source);
        this.artistRecord = artistRecord;
    }
}
