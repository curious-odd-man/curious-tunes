package com.github.curiousoddman.curious_tunes.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HistoryFormatTest {
    private static final ZoneId  ZONE = ZoneId.of("UTC");
    private static final Instant NOW  = Instant.parse("2026-10-29T15:00:00Z");

    private static OffsetDateTime at(String iso) {
        return OffsetDateTime.parse(iso);
    }

    @Test
    void todayShowsTheTime() {
        assertEquals("Today 14:03", HistoryFormat.playedAt(at("2026-10-29T14:03:22Z"), NOW, ZONE, Locale.ENGLISH));
    }

    @Test
    void yesterdayShowsTheTime() {
        assertEquals("Yesterday 21:15", HistoryFormat.playedAt(at("2026-10-28T21:15:00Z"), NOW, ZONE, Locale.ENGLISH));
    }

    @Test
    void olderShowsTheDate() {
        assertEquals("12 Oct 2026", HistoryFormat.playedAt(at("2026-10-12T08:00:00Z"), NOW, ZONE, Locale.ENGLISH));
    }

    @Test
    void theTimeZoneOfTheUserDecidesWhatTodayIs() {
        // 23:30 UTC on the 28th is already the 29th in UTC+2
        OffsetDateTime playedAt = at("2026-10-28T23:30:00Z");
        assertEquals("Today 01:30", HistoryFormat.playedAt(playedAt, NOW, ZoneId.of("Europe/Riga"), Locale.ENGLISH));
    }

    @Test
    void durationsUseTheTwoLargestUnits() {
        assertEquals("0 s", HistoryFormat.duration(0));
        assertEquals("59 s", HistoryFormat.duration(59));
        assertEquals("10 min", HistoryFormat.duration(600));
        assertEquals("1 h 1 min", HistoryFormat.duration(3660));
        assertEquals("1 d 1 h", HistoryFormat.duration(90_000));
        assertEquals("0 s", HistoryFormat.duration(-5));
    }

    @Test
    void offsetsOfTheStoredValueDoNotMatter() {
        OffsetDateTime inRiga = OffsetDateTime.of(2026, 10, 29, 16, 3, 0, 0, ZoneOffset.ofHours(2));   // 14:03 UTC
        assertEquals("Today 14:03", HistoryFormat.playedAt(inRiga, NOW, ZONE, Locale.ENGLISH));
    }
}
