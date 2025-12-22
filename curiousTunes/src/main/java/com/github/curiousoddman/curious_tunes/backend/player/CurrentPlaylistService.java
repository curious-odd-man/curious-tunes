package com.github.curiousoddman.curious_tunes.backend.player;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.event.AddToPlaylistEvent;
import com.github.curiousoddman.curious_tunes.event.ClearPlaylistEvent;
import com.github.curiousoddman.curious_tunes.event.RemoveFromPlaylistEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CurrentPlaylistService {
    private final List<TrackRecord> tracks;

    @EventListener
    public void onClearPlaylist(ClearPlaylistEvent clearPlaylistEvent) {
        tracks.clear();
    }

    @EventListener
    public void onAddToPlaylist(AddToPlaylistEvent addToPlaylistEvent) {
        tracks.addAll(addToPlaylistEvent.getTracks());
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
