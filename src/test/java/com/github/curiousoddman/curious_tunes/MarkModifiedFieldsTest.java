package com.github.curiousoddman.curious_tunes;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackOverridesHistoryRecord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.curiousoddman.curious_tunes.dbobj.Tables.TRACK;
import static com.github.curiousoddman.curious_tunes.ui.controller.element.tabs.LibraryTagEditTabController.wasOverridden;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// AI Generated
class MarkModifiedFieldsTest {

    // --- test doubles ---

    private TrackOverridesHistoryRecord override(String field, String oldValue) {
        TrackOverridesHistoryRecord r = mock(TrackOverridesHistoryRecord.class);
        when(r.getField()).thenReturn(field);
        when(r.getOldValue()).thenReturn(oldValue);
        return r;
    }

    // --- no overrides ---

    @Test
    void noOverrides_returnsNotModified() {
        assertFalse(wasOverridden("Rock", List.of(), TRACK.GENRE));
    }

    // --- override present, value changed ---

    @Test
    void overridePresent_valueDifferentFromOldValue_returnsModified() {
        var overrides = List.of(override("GENRE", "Pop"));
        assertTrue(wasOverridden("Rock", overrides, TRACK.GENRE));
    }

    @Test
    void overridePresent_valueMatchesOldValue_returnsNotModified() {
        var overrides = List.of(override("GENRE", "Rock"));
        assertFalse(wasOverridden("Rock", overrides, TRACK.GENRE));
    }

    // --- null handling ---

    @Test
    void currentValueNull_oldValueNotNull_returnsModified() {
        var overrides = List.of(override("GENRE", "Rock"));
        assertTrue(wasOverridden(null, overrides, TRACK.GENRE));
    }

    @Test
    void currentValueNull_oldValueNull_returnsNotModified() {
        var overrides = List.of(override("GENRE", null));
        assertFalse(wasOverridden(null, overrides, TRACK.GENRE));
    }

    @Test
    void currentValueNotNull_oldValueNull_returnsModified() {
        var overrides = List.of(override("GENRE", null));
        assertTrue(wasOverridden("Rock", overrides, TRACK.GENRE));
    }

    // --- multiple overrides, last one wins ---

    @Test
    void multipleOverrides_lastOneUsed() {
        // history: Pop → Rock → Jazz, current value is "Jazz" (matches last old value)
        var overrides = List.of(
                override("GENRE", "Pop"),
                override("GENRE", "Rock"),
                override("GENRE", "Jazz")
        );
        // current is "Jazz", last override oldValue is "Jazz" → not modified
        assertFalse(wasOverridden("Jazz", overrides, TRACK.GENRE));
    }

    @Test
    void multipleOverrides_lastOneIndicatesChange() {
        var overrides = List.of(
                override("GENRE", "Pop"),
                override("GENRE", "Rock"),
                override("GENRE", "Jazz")
        );
        // current is "Metal", last override oldValue is "Jazz" → modified
        assertTrue(wasOverridden("Metal", overrides, TRACK.GENRE));
    }

    // --- field name matching ---

    @Test
    void overrideForDifferentField_ignored() {
        var overrides = List.of(override("TITLE", "Old Title"));
        assertFalse(wasOverridden("Rock", overrides, TRACK.GENRE));
    }

    @Test
    void mixedFields_onlyMatchingFieldConsidered() {
        var overrides = List.of(
                override("TITLE", "Old Title"),
                override("GENRE", "Pop"),
                override("COMPOSER", "Bach")
        );
        assertTrue(wasOverridden("Rock", overrides, TRACK.GENRE));
    }
}