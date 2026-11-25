package com.github.curiousoddman.curious_tunes.domain;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.domain.audio.AlacStreaming;
import com.github.curiousoddman.curious_tunes.domain.audio.alac.AlacException;
import javafx.scene.media.Media;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaProvider {
    private final AlacStreaming alacStreaming;

    /**
     * The media to play a track with. JavaFX cannot decode ALAC, and {@code Media} only takes a URL, so an
     * {@code .m4a} ALAC track is streamed through the patched JavaFX media module ({@link AlacStreaming}, decoded
     * packet by packet as it plays). Everything else, including AAC {@code .m4a} files, plays from its file URI,
     * which JavaFX decodes itself.
     * <p>
     * There is no other way to play ALAC: if the patch isn't active (a JavaFX upgrade, a launch without the patch),
     * an ALAC track goes to JavaFX as a file and fails with the player's usual error notification.
     * <p>
     * Call {@link #release(Media)} when the player of the returned media is disposed.
     *
     * @param trackRecord track to be played
     * @return Media
     */
    public Media getMedia(TrackRecord trackRecord) {
        Path file = Path.of(trackRecord.getFileLocation());
        if (AudioFormat.of(file.toString())
                       .orElse(null) == AudioFormat.M4A && alacStreaming.isAvailable()) {
            try {
                return alacStreaming.open(file, trackRecord.getId());
            } catch (AlacException e) {
                log.debug("{} is not ALAC ({}); JavaFX plays it", file, e.getMessage());
            } catch (Exception e) {
                log.warn("Cannot stream {}; handing it to JavaFX as a file", file, e);
            }
        }
        return new Media(file.toUri()
                             .toString());
    }

    /**
     * Frees what {@link #getMedia} opened for {@code media} (for streamed ALAC: the file and its registration).
     */
    public void release(Media media) {
        alacStreaming.release(media);
    }
}
