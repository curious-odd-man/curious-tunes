package com.github.curiousoddman.curious_tunes.model.playlist;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Setter
@Getter
public class PlaylistItem extends TrackInfo {
    private PlaylistItemStatus playlistItemStatus = PlaylistItemStatus.QUEUED;

    public PlaylistItem(TrackRecord trackRecord, ArtistRecord trackArtist, AlbumRecord trackAlbum) {
        super(trackRecord, trackArtist, trackAlbum);
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof PlaylistItem that)) return false;

        return Objects.equals(trackRecord.getId(), that.trackRecord.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(trackRecord.getId());
    }
}
