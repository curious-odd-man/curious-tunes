package com.github.curiousoddman.curious_tunes.backend.lyrics;

import lombok.Getter;

@Getter
public class SongData {
    private final String artist;
    private final String album;
    private final String title;

    public SongData(String artist, String album, String title) {
        if (artist == null || album == null || title == null) {
            throw new IllegalArgumentException("artist, album and title must be not null");
        }
        this.artist = artist;
        this.album = album;
        this.title = title;
    }

    @Override
    public String toString() {
        return artist + "-" + album + "-" + title;
    }
}

