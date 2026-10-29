package com.github.curiousoddman.curious_tunes.model;

/**
 * Totals of the playback history of a period.
 *
 * @param plays        how many times a track was played
 * @param totalSeconds the time listened
 * @param albums       different albums that were played
 * @param artists      different artists that were played
 */
public record HistoryStats(long plays, long totalSeconds, long albums, long artists) {
    public static final HistoryStats EMPTY = new HistoryStats(0, 0, 0, 0);
}
