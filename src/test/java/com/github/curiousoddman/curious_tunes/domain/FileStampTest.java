package com.github.curiousoddman.curious_tunes.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStampTest {

    @Test
    void matchesOnlyWhenSizeAndTimeAreBothStoredAndEqual() {
        assertTrue(new FileStamp(1000L, 5L).matches(1000, 5));
        assertFalse(new FileStamp(1000L, 5L).matches(1001, 5), "size changed");
        assertFalse(new FileStamp(1000L, 5L).matches(1000, 6), "time changed");
        assertFalse(new FileStamp(null, 5L).matches(1000, 5), "never stored: scan once");
        assertFalse(new FileStamp(1000L, null).matches(1000, 5), "never stored: scan once");
    }
}
