package com.github.curiousoddman.curious_tunes.model;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * What is being dragged from the library to the queue. A JavaFX dragboard can only carry simple data, so the
 * tracks or albums are kept here while the drag lasts.
 */
@Component
public class LibraryDragPayload {
    private List<TrackInfo>   tracks = List.of();
    private List<AlbumRecord> albums = List.of();

    public void setTracks(List<TrackInfo> tracks) {
        this.tracks = List.copyOf(tracks);
        this.albums = List.of();
    }

    public void setAlbums(List<AlbumRecord> albums) {
        this.albums = List.copyOf(albums);
        this.tracks = List.of();
    }

    public boolean isActive() {
        return !tracks.isEmpty() || !albums.isEmpty();
    }

    public List<TrackInfo> getTracks() {
        return tracks;
    }

    public List<AlbumRecord> getAlbums() {
        return albums;
    }

    public void clear() {
        tracks = List.of();
        albums = List.of();
    }
}
