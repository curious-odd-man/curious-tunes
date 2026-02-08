package com.github.curiousoddman.curious_tunes.backend.lyrics.sources;

import com.github.curiousoddman.curious_tunes.backend.lyrics.RequestUtils;
import com.github.curiousoddman.curious_tunes.backend.lyrics.SongData;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class DarkLyrics implements LyricsSource {

    private static final Pattern REGEX_NON_ALPHANUMERIC =
            Pattern.compile("[^a-z0-9]+");

    private static final Pattern STARTS_WITH_NUMBER_PATTERN =
            Pattern.compile("^\\d+\\.\\s");

    private static final Map<String, String> REQUEST_HEADERS =
            RequestUtils.getRequestHeaders();

    @Override
    public boolean isAlbum() {
        return true;
    }

    @Override
    public Map.Entry<String, Map<String, String>> prepareRequest(SongData songData) {
        String artist = REGEX_NON_ALPHANUMERIC
                .matcher(songData.getArtist().toLowerCase())
                .replaceAll("");

        String album = REGEX_NON_ALPHANUMERIC
                .matcher(songData.getAlbum().toLowerCase())
                .replaceAll("");

        String url = String.format(
                "http://www.darklyrics.com/lyrics/%s/%s.html",
                artist,
                album
        );

        return Map.entry(url, REQUEST_HEADERS);
    }

    private static String formatSong(String currentSong) {
        String lowerCaseSongName = currentSong.toLowerCase();
        return STARTS_WITH_NUMBER_PATTERN
                .matcher(lowerCaseSongName)
                .replaceAll("");
    }

    @Override
    public String parseLyrics(String html, String songTitle) {
        Document soup = Jsoup.parse(html);
        Map<String, String> songsWithLyrics = splitBySongs(soup);

        if (songsWithLyrics == null || songsWithLyrics.isEmpty()) {
            return null;
        }

        String key = formatSong(songTitle);
        String songLyrics = songsWithLyrics.get(key);

        if (songLyrics == null) {
            return null;
        }

        if (songLyrics.isEmpty()) {
            return "[Instrumental]";
        }

        return songLyrics;
    }

    @Override
    public String getName() {
        return "Dark_Lyrics";
    }

    private Map<String, String> splitBySongs(Document soup) {
        Elements lyricsDivs = soup.select("div.lyrics");
        if (lyricsDivs.isEmpty()) {
            return null;
        }

        Map<String, String> songsWithLyrics = new HashMap<>();
        String currentSong = null;
        StringBuilder currentLyrics = new StringBuilder();

        for (Element lyricsDiv : lyricsDivs) {
            for (Element element : lyricsDiv.children()) {
                if ("h3".equals(element.tagName())) {
                    if (currentSong != null) {
                        songsWithLyrics.put(
                                formatSong(currentSong),
                                currentLyrics.toString().trim()
                        );
                    }
                    currentSong = element.text();
                    currentLyrics.setLength(0);
                } else {
                    currentLyrics.append(element.text());
                }
            }
        }

        if (currentSong != null && !currentLyrics.isEmpty()) {
            songsWithLyrics.put(
                    formatSong(currentSong),
                    currentLyrics.toString().trim()
            );
        }

        return songsWithLyrics;
    }
}
