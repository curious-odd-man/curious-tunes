package com.github.curiousoddman.curious_tunes.model;

import java.time.OffsetDateTime;

/**
 * One play of a track, with the names that are shown in the history table.
 */
public record HistoryRow(OffsetDateTime playedAt, int trackId, String title, String album, String artist) {
}
