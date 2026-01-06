package com.github.curiousoddman.curious_tunes.backend;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.event.player.PlayedThirdOfTrackEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaybackHistoryService {
    private final DataAccess dataAccess;

    @EventListener
    public void onThirdOfSongPlayed(PlayedThirdOfTrackEvent event) {
        TrackRecord trackRecord = event.getTrackRecord();
        dataAccess.insertIntoHistory(trackRecord);
    }
}
