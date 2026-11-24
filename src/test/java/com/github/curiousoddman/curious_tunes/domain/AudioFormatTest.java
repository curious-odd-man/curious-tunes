package com.github.curiousoddman.curious_tunes.domain;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AudioFormatTest {

    @Test
    void recognisesExtensionsWhateverTheirCase() {
        assertEquals(Optional.of(AudioFormat.MP3), AudioFormat.of(Path.of("music", "Song.mp3")));
        assertEquals(Optional.of(AudioFormat.MP3), AudioFormat.of(Path.of("music", "Song.MP3")));
        assertEquals(Optional.of(AudioFormat.M4A), AudioFormat.of(Path.of("music", "01 Track.M4A")));
        assertEquals(Optional.of(AudioFormat.M4A), AudioFormat.of("C:\\iTunes\\Music\\a.b\\c.m4a"));
    }

    @Test
    void rejectsEverythingElse() {
        assertFalse(AudioFormat.isAudioFile(Path.of("music", "cover.jpg")));
        assertFalse(AudioFormat.isAudioFile(Path.of("music", "Folder.ini")));
        assertFalse(AudioFormat.isAudioFile(Path.of("music", "noextension")));
        assertFalse(AudioFormat.isAudioFile(Path.of("music", "trailingdot.")));
        assertFalse(AudioFormat.isAudioFile(Path.of("music", "song.mp3.part")));
        assertFalse(AudioFormat.isAudioFile(Path.of("music", "protected.m4p")));
        assertFalse(AudioFormat.isAudioFile(Path.of("mp3")));            // a name, not an extension
    }
}
