package com.github.curiousoddman.curious_tunes.backend.player;

import com.github.curiousoddman.curious_tunes.backend.DataAccess;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.event.AddToPlaylistEvent;
import com.github.curiousoddman.curious_tunes.event.ClearPlaylistEvent;
import com.github.curiousoddman.curious_tunes.event.RemoveFromPlaylistEvent;
import com.github.curiousoddman.curious_tunes.model.PlaylistAddMode;
import com.github.curiousoddman.curious_tunes.model.Shuffle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CurrentPlaylistService {
    private final DataAccess dataAccess;
    private final List<TrackRecord> tracks;

    @EventListener
    public void onClearPlaylist(ClearPlaylistEvent clearPlaylistEvent) {
        tracks.clear();
    }

    @EventListener
    public void onAddToPlaylist(AddToPlaylistEvent addToPlaylistEvent) {
        List<TrackRecord> tracksToAdd = new ArrayList<>();
        if (addToPlaylistEvent.getTracks() != null) {
            tracksToAdd.addAll(addToPlaylistEvent.getTracks());
        }
        if (addToPlaylistEvent.getArtistRecord() != null) {
            List<TrackRecord> artistTracks = dataAccess.getArtistTracks(addToPlaylistEvent.getArtistRecord());
            tracksToAdd.addAll(artistTracks);
        }
        if (addToPlaylistEvent.getAlbums() != null) {
            List<TrackRecord> artistTracks = dataAccess.getAlbumsTracks(addToPlaylistEvent.getAlbums());
            tracksToAdd.addAll(artistTracks);
        }
        if (addToPlaylistEvent.getPlaylistAddMode() == PlaylistAddMode.REPLACE) {
            tracks.clear();
        }
        Shuffle shuffle = addToPlaylistEvent.getShuffle();
        if (shuffle == Shuffle.SKIP) {
            tracks.addAll(tracksToAdd);
        } else if (shuffle == Shuffle.BEFORE_ADDING_TO_PLAYLIST) {
            Collections.shuffle(tracksToAdd);
            tracks.addAll(tracksToAdd);
        } else if (shuffle == Shuffle.AFTER_ADDING_TO_PLAYLIST) {
            tracks.addAll(tracksToAdd);
            Collections.shuffle(tracks);
        } else {
            log.error("Unknown type of shuffle {}", shuffle);
            tracks.addAll(tracksToAdd);
        }
    }

    @EventListener
    public void onRemoveFromPlaylist(RemoveFromPlaylistEvent removeFromPlaylistEvent) {
        tracks.removeAll(removeFromPlaylistEvent.getTracks());
    }

    public TrackRecord getCurrentTrack() {
        return null;    // TODO
    }

    public TrackRecord getNextTrack() {
        return null; // TODO
    }

    public TrackRecord getPreviousTrack() {
        return null; // TODO
    }
}
