package com.github.curiousoddman.curious_tunes.backend.lyrics.sources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.curiousoddman.curious_tunes.backend.lyrics.LyricsHelper;
import com.github.curiousoddman.curious_tunes.backend.lyrics.RequestUtils;
import com.github.curiousoddman.curious_tunes.backend.lyrics.SongData;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Slf4j
@Component
public class LyricsnMusic implements LyricsSource {
    private static final Pattern CARRIAGE_RETURN_PATTERN =
            Pattern.compile("\\r");

    private static final OkHttpClient CLIENT = new OkHttpClient();

    private static final Map<String, String> REQUEST_HEADERS =
            RequestUtils.getRequestHeaders();

    private static final String API_KEY =
            RequestUtils.getLnmApiKey();

    static {
        REQUEST_HEADERS.put("Content-type", "application/json");
    }

    @Override
    public boolean isAlbum() {
        return false;
    }

    @SneakyThrows
    @Override
    public Map.Entry<String, Map<String, String>> prepareRequest(SongData songData) {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse("http://api.lyricsnmusic.com/songs"))
                .newBuilder()
                .addQueryParameter("api_key", API_KEY)
                .addQueryParameter("artist", songData.getArtist())
                .addQueryParameter("track", songData.getTitle())
                .build();

        Request.Builder requestBuilder = new Request.Builder()
                .url(url);

        REQUEST_HEADERS.forEach(requestBuilder::addHeader);

        try (Response response = CLIENT.newCall(requestBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response " + response);
            }

            String body = response.body().string();
            var respJson = new ObjectMapper().readTree(body);

            if (respJson == null) {
                log.info("Empty response");
                return null;
            }

            String lyricsUrl = respJson.get(0).get("url").asText();
            if (lyricsUrl == null) {
                log.info("No lyrics url returned");
                return null;
            }

            REQUEST_HEADERS.remove("Content-type");
            return Map.entry(lyricsUrl, new HashMap<>(REQUEST_HEADERS));
        }
    }

    @Override
    public String parseLyrics(String html, String songTitle) {

        Document soup = Jsoup.parse(html);
        Element mainDiv = soup.getElementById("main");

        if (mainDiv != null) {
            Element pre = mainDiv.selectFirst("pre");
            if (pre != null) {
                String lyrics = pre.text().trim();

                // remove superfluous '\r'
                lyrics = CARRIAGE_RETURN_PATTERN.matcher(lyrics).replaceAll("");

                if (LyricsHelper.testLyrics(lyrics)) {
                    return lyrics;
                } else {
                    log.info("Failed to verify lyrics contents.");
                    return null;
                }
            }
        }

        log.info("Unable to find lyrics on page");
        return null;
    }

    @Override
    public String getName() {
        return "Lyrics_n_Music";
    }
}
