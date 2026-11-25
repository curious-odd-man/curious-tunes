package com.github.curiousoddman.curious_tunes.domain.lyrics;

public record SongData(String artist, String album, String title) {
    public SongData {
        if (artist == null || album == null || title == null) {
            throw new IllegalArgumentException("artist, album and title must be not null");
        }
    }

    @Override
    public String toString() {
        return artist + "-" + album + "-" + title;
    }
}

