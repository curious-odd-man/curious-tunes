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

    // --- multiple overrides: the first one tells the original value (the one that came from the file) ---

    @Test
    void multipleOverrides_backToTheOriginalIsNotModified() {
        // history: Pop → Rock → Jazz; the first edit replaced "Pop", so "Pop" is the value from the file
        var overrides = List.of(
                override("GENRE", "Pop"),
                override("GENRE", "Rock"),
                override("GENRE", "Jazz")
        );
        assertFalse(wasOverridden("Pop", overrides, TRACK.GENRE));
    }

    @Test
    void multipleOverrides_differentFromTheOriginalIsModified() {
        var overrides = List.of(
                override("GENRE", "Pop"),
                override("GENRE", "Rock"),
                override("GENRE", "Jazz")
        );
        // the value that was replaced last ("Jazz") is not the original, so it still counts as edited
        assertTrue(wasOverridden("Jazz", overrides, TRACK.GENRE));
        assertTrue(wasOverridden("Metal", overrides, TRACK.GENRE));
    }

    // --- values of other types are compared as text ---

    @Test
    void numberIsComparedWithTheTextOfTheOldValue() {
        var overrides = List.of(override("TRACK_NUMBER", "5"));
        assertFalse(wasOverridden(5, overrides, TRACK.TRACK_NUMBER));
        assertTrue(wasOverridden(6, overrides, TRACK.TRACK_NUMBER));
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