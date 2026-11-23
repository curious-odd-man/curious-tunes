package com.github.curiousoddman.curious_tunes.util;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProcessPriorityTest {

    @Test
    void parsesTheLevelsInTheUsualSpellings() {
        assertEquals(Optional.of(ProcessPriority.Level.NORMAL), ProcessPriority.parse("normal"));
        assertEquals(Optional.of(ProcessPriority.Level.ABOVE_NORMAL), ProcessPriority.parse("above_normal"));
        assertEquals(Optional.of(ProcessPriority.Level.ABOVE_NORMAL), ProcessPriority.parse("Above-Normal"));
        assertEquals(Optional.of(ProcessPriority.Level.ABOVE_NORMAL), ProcessPriority.parse("aboveNormal"));
        assertEquals(Optional.of(ProcessPriority.Level.HIGH), ProcessPriority.parse(" HIGH "));
    }

    @Test
    void refusesUnknownLevelsAndRealtime() {
        assertEquals(Optional.empty(), ProcessPriority.parse("realtime"));
        assertEquals(Optional.empty(), ProcessPriority.parse("fast"));
        assertEquals(Optional.empty(), ProcessPriority.parse(""));
        assertEquals(Optional.empty(), ProcessPriority.parse(null));
    }
}
