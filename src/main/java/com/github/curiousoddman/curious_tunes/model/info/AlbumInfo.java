package com.github.curiousoddman.curious_tunes.model.info;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class AlbumInfo {
    protected final ArtistRecord artistRecord;
    protected final AlbumRecord albumRecord;

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
