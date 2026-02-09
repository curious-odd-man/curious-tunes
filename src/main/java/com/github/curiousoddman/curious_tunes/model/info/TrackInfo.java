package com.github.curiousoddman.curious_tunes.model.info;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class TrackInfo {
    protected final TrackRecord trackRecord;
    protected final ArtistRecord trackArtist;
    protected final AlbumRecord trackAlbum;

    public Integer getTrackNumber() {
        return trackRecord.getTrackNumber();
    }

    public String getTitle() {
        return trackRecord.getTitle();
    }

    public long getDuration() {
        return trackRecord.getDuration();
    }

    public String getLyrics() {
        return trackRecord.getLyrics();
    }
}
