package com.github.curiousoddman.curious_tunes.model.info;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;

public record AlbumInfo(ArtistRecord artistRecord, AlbumRecord albumRecord, Integer albumYear) {
    public TrackInfo toTrackInfo(TrackRecord trackRecord) {
        return new TrackInfo(
                trackRecord,
                artistRecord,
                albumRecord
        );
    }

    public String getName() {
        return albumRecord.getName();
    }

    public int getId() {
        return albumRecord.getId();
    }

    public byte[] getImage() {
        return albumRecord.getImage();
    }
}
