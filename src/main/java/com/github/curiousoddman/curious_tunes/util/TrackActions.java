package com.github.curiousoddman.curious_tunes.util;

import com.github.curiousoddman.curious_tunes.event.AddToPlaylistEvent;
import com.github.curiousoddman.curious_tunes.event.PlaySpecificTrackEvent;
import com.github.curiousoddman.curious_tunes.model.Shuffle;
import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistAddMode;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

/**
 * Things that can be done with a few library tracks.
 */
public final class TrackActions {
    private TrackActions() {
    }

    /**
     * Plays the tracks right away: they are put after the current queue item and the first one is started.
     */
    public static void playNow(ApplicationEventPublisher publisher, Object source, List<TrackInfo> tracks) {
        if (tracks.isEmpty()) {
            return;
        }
        if (tracks.size() == 1) {
            publisher.publishEvent(new PlaySpecificTrackEvent(source, tracks.getFirst(), true));
            return;
        }
        publisher.publishEvent(addEvent(source, tracks, PlaylistAddMode.PUT_AFTER_CURRENT));
        publisher.publishEvent(new PlaySpecificTrackEvent(source, tracks.getFirst(), false));
    }

    public static void playNext(ApplicationEventPublisher publisher, Object source, List<TrackInfo> tracks) {
        if (!tracks.isEmpty()) {
            publisher.publishEvent(addEvent(source, tracks, PlaylistAddMode.PUT_AFTER_CURRENT));
        }
    }

    public static void addToQueue(ApplicationEventPublisher publisher, Object source, List<TrackInfo> tracks) {
        if (!tracks.isEmpty()) {
            publisher.publishEvent(addEvent(source, tracks, PlaylistAddMode.APPEND));
        }
    }

    private static AddToPlaylistEvent addEvent(Object source, List<TrackInfo> tracks, PlaylistAddMode mode) {
        return AddToPlaylistEvent
                .builder()
                .source(source)
                .tracks(List.copyOf(tracks))
                .albums(List.of())
                .shuffle(Shuffle.SKIP)
                .playlistAddMode(mode)
                .build();
    }
}
