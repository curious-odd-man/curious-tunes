package com.github.curiousoddman.curious_tunes.event;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.model.PlaylistAddMode;
import com.github.curiousoddman.curious_tunes.model.Shuffle;
import lombok.Builder;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
@Builder
public class AddToPlaylistEvent extends ApplicationEvent {
    private final Object source;        // Duplicate this for @Builder to work
    private final List<TrackQueueItem> tracks;
    private final List<AlbumRecord> albums;
    private final ArtistRecord artistRecord;
    private final Shuffle shuffle;
    private final PlaylistAddMode playlistAddMode;

    public AddToPlaylistEvent(Object source, List<TrackQueueItem> tracks, List<AlbumRecord> albums, ArtistRecord artistRecord, Shuffle shuffle, PlaylistAddMode playlistAddMode) {
        super(source);
        this.source = source;
        this.tracks = tracks;
        this.albums = albums;
        this.artistRecord = artistRecord;
        this.shuffle = shuffle;
        this.playlistAddMode = playlistAddMode;
    }

    public record TrackQueueItem(TrackRecord trackRecord, ArtistRecord trackArtist, AlbumRecord trackAlbum) {
    }
}
