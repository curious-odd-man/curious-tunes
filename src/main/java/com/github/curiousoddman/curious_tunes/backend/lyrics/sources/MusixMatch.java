package com.github.curiousoddman.curious_tunes.backend.lyrics.sources;

import com.github.curiousoddman.curious_tunes.backend.lyrics.LyricsHelper;
import com.github.curiousoddman.curious_tunes.backend.lyrics.RequestUtils;
import com.github.curiousoddman.curious_tunes.backend.lyrics.SongData;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class MusixMatch implements LyricsSource {
    private static final Pattern REGEX_NON_ALPHANUM =
            Pattern.compile("[^\\p{L}\\p{N} \\-]+");

    private static final Pattern REGEX_SPACES =
            Pattern.compile("\\s+");

    private static final Map<String, String> REQUEST_HEADERS =
            RequestUtils.getRequestHeaders();


    @Override
    public boolean isAlbum() {
        return false;
    }

    @Override
    public Map.Entry<String, Map<String, String>> prepareRequest(SongData songData) {

        String refinedArtist = refineText(songData.getArtist());
        String refinedTitle = refineText(songData.getTitle());

        String url = String.format(
                "https://www.musixmatch.com/lyrics/%s/%s",
                urlEncode(refinedArtist),
                urlEncode(refinedTitle)
        );

        return Map.entry(url, REQUEST_HEADERS);
    }

    private static String refineText(String rawString) {
        if (rawString == null) {
            return "";
        }

        // Replace apostrophes with dashes
        String result = rawString.replace("'", "-");

        // Remove non-alphanumeric (unicode aware), except spaces and dashes
        result = REGEX_NON_ALPHANUM.matcher(result).replaceAll("");

        // Replace spaces with dashes
        result = REGEX_SPACES.matcher(result).replaceAll("-");

        return result;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @Override
    public String parseLyrics(String html, String songTitle) {

        Document soup = Jsoup.parse(html);
        StringBuilder lyricsBuilder = new StringBuilder();

        Elements paragraphs =
                soup.select("p.mxm-lyrics__content");

        for (Element p : paragraphs) {
            lyricsBuilder.append(p.text().strip());
        }

        String lyrics = lyricsBuilder.toString();

        if (LyricsHelper.testLyrics(lyrics)) {
            return lyrics;
        }

        return null;
    }

    @Override
    public String getName() {
        return "Musix Match";
    }
}
