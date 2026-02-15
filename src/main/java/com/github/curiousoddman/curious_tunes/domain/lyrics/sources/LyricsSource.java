package com.github.curiousoddman.curious_tunes.domain.lyrics.sources;

import com.github.curiousoddman.curious_tunes.domain.lyrics.SongData;

import java.util.Map;

public interface LyricsSource {
    boolean isAlbum();

    Map.Entry<String, Map<String, String>> prepareRequest(SongData songData);

    String parseLyrics(String html, String songTitle);

    String getName();
}
