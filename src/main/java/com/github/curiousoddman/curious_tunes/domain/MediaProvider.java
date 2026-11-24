package com.github.curiousoddman.curious_tunes.domain;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.domain.audio.AlacStreaming;
import javafx.scene.media.Media;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaProvider {
    @Value("${server.port}")
    private final String        serverPort;
    private final AlacStreaming alacStreaming;

    /**
     * The media to play a track with. JavaFX cannot decode ALAC, and {@code Media} only takes a URL, so an
     * {@code .m4a} (ALAC) track is either streamed through the patched JavaFX media module ({@link AlacStreaming},
     * decoded packet by packet as it plays) or, when the patch isn't active or the file can't be streamed,
     * decoded by the local HTTP endpoint (the fallback). Other files play from their file URI.
     * <p>
     * Call {@link #release(Media)} when the player of the returned media is disposed.
     *
     * @param trackRecord track to be played
     * @return Media
     */
    public Media getMedia(TrackRecord trackRecord) {
        String fileLocation = trackRecord.getFileLocation();
        if (AudioFormat.of(fileLocation)
                       .orElse(null) != AudioFormat.M4A) {
            return new Media(Path.of(fileLocation)
                                 .toUri()
                                 .toString());
        }
        if (alacStreaming.isAvailable()) {
            try {
                return alacStreaming.open(Path.of(fileLocation), trackRecord.getId());
            } catch (Exception e) {
                log.warn("Cannot stream {}; playing it through the fallback", fileLocation, e);
            }
        }
        URI fallback = URI.create("http://localhost:" + serverPort + "/current/track?name="
                + URLEncoder.encode(fileLocation, StandardCharsets.UTF_8));
        return new Media(fallback.toString());
    }

    /**
     * Frees what {@link #getMedia} opened for {@code media} (for streamed ALAC: the file and its registration).
     */
    public void release(Media media) {
        alacStreaming.release(media);
    }
}
