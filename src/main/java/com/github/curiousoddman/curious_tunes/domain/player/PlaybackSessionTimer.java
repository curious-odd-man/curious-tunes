package com.github.curiousoddman.curious_tunes.domain.player;

import com.github.curiousoddman.curious_tunes.event.player.PlayerStatusEvent;
import com.github.curiousoddman.curious_tunes.event.player.PlaylistResetEvent;
import com.github.curiousoddman.curious_tunes.model.PlaybackState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.OptionalLong;

/**
 * Tracks how long the current listening session has been going (wall clock).
 * <ul>
 *     <li>starts with the first playback,</li>
 *     <li>is not affected by pauses, seeks, or adding tracks to the playlist,</li>
 *     <li>is reset when the playlist is fully replaced or cleared.</li>
 * </ul>
 */
@Slf4j
@Component
public class PlaybackSessionTimer {
    private final SessionClock clock = new SessionClock();

    @EventListener
    public void onPlayerStatus(PlayerStatusEvent event) {
        PlaybackState status = event.getStatus();
        if (status == PlaybackState.LAUNCHING || status == PlaybackState.PLAYING) {
            if (!clock.isStarted()) {
                log.info("Listening session started");
            }
            clock.startIfNotStarted();
        }
    }

    @EventListener
    public void onPlaylistReset(PlaylistResetEvent event) {
        log.info("Playlist was replaced/cleared - listening session reset");
        clock.reset();
    }

    public OptionalLong getElapsedSeconds() {
        return clock.elapsedSeconds();
    }

    /**
     * @return formatted session time, or an empty string if the session has not started yet
     */
    public String getDisplayText() {
        OptionalLong elapsed = clock.elapsedSeconds();
        return elapsed.isPresent() ? SessionClock.format(elapsed.getAsLong()) : "";
    }
}
