package com.github.curiousoddman.curious_tunes.domain;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/**
 * The audio file types the library supports, recognised by file extension, case-insensitively
 * ({@code Song.MP3} is an MP3 like {@code song.mp3}). The one place that decides what counts as an audio file:
 * used by the library scan, the metadata reader and the media provider, so they cannot disagree.
 */
public enum AudioFormat {
    MP3("mp3"),
    /**
     * MPEG-4 audio. In this library always Apple Lossless (ALAC); see doc/performance-review.md, item 1.1.
     */
    M4A("m4a");

    private final String extension;

    AudioFormat(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }

    public static Optional<AudioFormat> of(Path file) {
        Path name = file.getFileName();
        return name == null ? Optional.empty() : of(name.toString());
    }

    /**
     * @param fileName a file name or path; only the part after the last dot counts
     */
    public static Optional<AudioFormat> of(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return Optional.empty();
        }
        String ext = fileName.substring(dot + 1)
                             .toLowerCase(Locale.ROOT);
        for (AudioFormat format : values()) {
            if (format.extension.equals(ext)) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }

    public static boolean isAudioFile(Path file) {
        return of(file).isPresent();
    }
}
