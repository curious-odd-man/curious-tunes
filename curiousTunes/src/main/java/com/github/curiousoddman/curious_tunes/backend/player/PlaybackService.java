package com.github.curiousoddman.curious_tunes.backend.player;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.event.PlayNextEvent;
import com.github.curiousoddman.curious_tunes.event.PlayPauseEvent;
import com.github.curiousoddman.curious_tunes.event.PlayPreviousEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaybackService {
    private final ApplicationEventPublisher applicationEventPublisher;
    private final CurrentPlaylistService currentPlaylistService;

    @EventListener
    public void onPlayPause(PlayPauseEvent playPauseEvent) {
        TrackRecord trackRecord = currentPlaylistService.getCurrentTrack();
        // TODO
    }

    @EventListener
    public void onNextEvent(PlayNextEvent playNextEvent) {
        TrackRecord trackRecord = currentPlaylistService.getNextTrack();
        // TODO
    }

    @EventListener
    public void onPreviousEvent(PlayPreviousEvent playPreviousEvent) {
        TrackRecord trackRecord = currentPlaylistService.getPreviousTrack();
        // TODO
    }
}
