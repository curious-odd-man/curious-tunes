package com.github.curiousoddman.curious_tunes.backend.lyrics.sources;

import com.github.curiousoddman.curious_tunes.backend.lyrics.LyricsHelper;
import com.github.curiousoddman.curious_tunes.backend.lyrics.RequestUtils;
import com.github.curiousoddman.curious_tunes.backend.lyrics.SongData;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Component
public class LyricsMode implements LyricsSource {
    private static final Pattern REGEX_NON_ALPHANUMERIC =
            Pattern.compile("[^a-z0-9]+");

    private static final Pattern REGEX_UNDERSCORES =
            Pattern.compile("_+");

    private static final Set<Character> LOWERCASE_CHARS =
            Set.of(
                    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
                    'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
            );

    private static final Map<String, String> REQUEST_HEADERS =
            RequestUtils.getRequestHeaders();

    @Override
    public boolean isAlbum() {
        return false;
    }

    @Override
    public Map.Entry<String, Map<String, String>> prepareRequest(SongData songData) {

        String artist = LyricsHelper.removeAccents(songData.getArtist())
                .toLowerCase();
        artist = REGEX_NON_ALPHANUMERIC.matcher(artist).replaceAll("");
        artist = REGEX_UNDERSCORES.matcher(artist).replaceAll("_");

        String title = songData.getTitle().toLowerCase();
        title = REGEX_NON_ALPHANUMERIC.matcher(title).replaceAll("");
        title = REGEX_UNDERSCORES.matcher(title).replaceAll("_");

        // If the artist is empty, pad with space
        if (artist.isEmpty()) {
            artist = " ";
        }

        char firstArtistChar = artist.charAt(0);
        String firstCharFolder =
                LOWERCASE_CHARS.contains(firstArtistChar)
                        ? String.valueOf(firstArtistChar)
                        : "0-9";

        String url = String.format(
                "http://www.lyricsmode.com/lyrics/%s/%s/%s.html",
                firstCharFolder,
                artist,
                title
        );

        return Map.entry(url, REQUEST_HEADERS);
    }

    @Override
    public String parseLyrics(String html, String songTitle) {

        Document soup = Jsoup.parse(html);

        // Lyrics are inside a div with id 'lyrics_text'
        Element lyricsText = soup.getElementById("lyrics_text");
        String lyrics = lyricsText != null
                ? lyricsText.text().trim()
                : null;

        if (LyricsHelper.testLyrics(lyrics)) {
            return lyrics;
        } else {
            log.info("Failed to verify lyrics");
            return null;
        }
    }

    @Override
    public String getName() {
        return "Lyrics_Mode";
    }
}
