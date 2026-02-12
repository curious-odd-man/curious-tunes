package com.github.curiousoddman.curious_tunes.backend.lyrics;

import com.github.curiousoddman.curious_tunes.backend.lyrics.sources.LyricsSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class LyricsService {
    private static final OkHttpClient CLIENT = new OkHttpClient();

    private final List<LyricsSource> sourceList;

    public void findLyricsAsync(String artist,
                                String album,
                                String title,
                                Consumer<String> onSuccess,
                                Runnable onFailure) {
        Thread t = new Thread(() -> {
            Optional<String> lyrics = findLyrics(artist, album, title);
            if (lyrics.isEmpty()) {
                onFailure.run();
            } else {
                onSuccess.accept(lyrics.get());
            }
        }, "Find lyrics online");
        t.start();
    }

    public Optional<String> findLyrics(String artist, String album, String title) {
        log.info("Attempting to find lyrics...");
        List<LyricsSource> shuffled = new ArrayList<>(sourceList);
        Collections.shuffle(shuffled);
        SongData songData = new SongData(
                artist, album, title
        );
        for (LyricsSource lyricsSource : shuffled) {
            try {
                log.info("Trying {}", lyricsSource.getName());
                Map.Entry<String, Map<String, String>> urlAndHeaders = lyricsSource.prepareRequest(songData);
                Request.Builder requestBuilder = new Request.Builder().url(urlAndHeaders.getKey());

                urlAndHeaders.getValue().forEach(requestBuilder::addHeader);

                try (Response response = CLIENT.newCall(requestBuilder.build()).execute()) {
                    if (!response.isSuccessful()) {
                        log.error("Fail {}", response);
                        continue;
                    }

                    String lyrics = lyricsSource.parseLyrics(response.body().string(), title);
                    if (lyrics != null) {
                        return Optional.of(lyrics);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        log.info("Completed lyrics search.");
        return Optional.empty();
    }
}
