package com.github.curiousoddman.curious_tunes.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AvatarTest {
    @Test
    void initialsOfTwoWords() {
        assertEquals("PF", Avatar.initials("Pink Floyd"));
        assertEquals("AC", Avatar.initials("  ab   cd  "));
        assertEquals("TD", Avatar.initials("The (Dead) Weather"));
    }

    @Test
    void initialsOfOneWordUseTwoLetters() {
        assertEquals("QU", Avatar.initials("Queen"));
        assertEquals("QU", Avatar.initials("(Queen)"));
        assertEquals("A", Avatar.initials("A"));
    }

    @Test
    void namesWithoutLettersGetAQuestionMark() {
        assertEquals("?", Avatar.initials("!!!"));
        assertEquals("?", Avatar.initials(""));
        assertEquals("?", Avatar.initials(null));
    }

    @Test
    void colorIsStablePerName() {
        assertEquals(Avatar.color("Queen"), Avatar.color("queen"));
        assertNotEquals(Avatar.color("Queen"), Avatar.color("Abba"));
    }
}
