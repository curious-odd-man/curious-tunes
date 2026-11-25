package com.github.curiousoddman.curious_tunes.domain.audio;

import com.github.curiousoddman.curious_tunes.domain.audio.alac.AlacVirtualWav;
import com.sun.media.jfxmedia.locator.RandomAccessSource;
import com.sun.media.jfxmedia.locator.StreamSources;
import javafx.scene.media.Media;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Plays ALAC files straight from the file, decoded packet by packet as the player reads them
 * (doc/performance-review.md, section 1, option G): each track becomes an {@link AlacVirtualWav}, registered with
 * the patched JavaFX media module ({@link StreamSources}) under a URI that names no real file.
 * <p>
 * {@link #isAvailable()} says whether the patch is in place. If it isn't (a JavaFX upgrade, a launch without the
 * patch), ALAC files cannot be played: there is no fallback since the HTTP endpoint was removed.
 */
@Slf4j
@Component
public class AlacStreaming {
    private static final String URI_PREFIX = "file:///ctunes-stream/";

    private final AtomicLong        counter = new AtomicLong();
    private final Map<Media, Entry> open    = new IdentityHashMap<>();
    private volatile Boolean        available;

    private record Entry(URI uri, AlacVirtualWav wav) {
    }

    /**
     * @return true when the patched javafx.media classes are the ones in use; checked once, then remembered
     */
    public boolean isAvailable() {
        Boolean result = available;
        if (result == null) {
            boolean active;
            try {
                active = StreamSources.isPatchActive();
            } catch (LinkageError e) {          // the patch classes aren't on the class path at all
                active = false;
            }
            result = active;
            available = result;
            if (active) {
                log.info("ALAC streaming: JavaFX media patch {} active", StreamSources.PATCH_VERSION);
            } else {
                log.error("ALAC streaming: JavaFX media patch not active; ALAC files will not play. Was the patch built for JavaFX {}?",
                        StreamSources.PATCH_VERSION);
            }
        }
        return result;
    }

    /**
     * Opens the file and returns a {@code Media} that plays it. Call {@link #release(Media)} when the player is
     * disposed.
     *
     * @throws IOException                                                     if the file cannot be read
     * @throws com.github.curiousoddman.curious_tunes.domain.audio.alac.AlacException if it isn't ALAC this decoder
     *                                                                         supports
     */
    public Media open(Path file, Object trackId) throws IOException {
        AlacVirtualWav wav = AlacVirtualWav.open(file);
        URI            uri = URI.create(URI_PREFIX + trackId + "-" + counter.incrementAndGet() + ".wav");
        StreamSources.register(uri, new Source(wav));
        try {
            Media media = new Media(uri.toString());
            synchronized (open) {
                open.put(media, new Entry(uri, wav));
            }
            log.debug("Streaming {} as {} ({} frames, {} Hz, {}-bit)", file, uri, wav.totalFrames(), wav.sampleRate(),
                    wav.bitsPerSample());
            return media;
        } catch (RuntimeException e) {
            StreamSources.unregister(uri);
            wav.close();
            throw e;
        }
    }

    /**
     * Unregisters the source of {@code media} and closes its file. Does nothing for other media.
     */
    public void release(Media media) {
        if (media == null) {
            return;
        }
        Entry entry;
        synchronized (open) {
            entry = open.remove(media);
        }
        if (entry == null) {
            return;
        }
        StreamSources.unregister(entry.uri());
        try {
            entry.wav()
                 .close();
        } catch (IOException e) {
            log.debug("Closing {}", entry.uri(), e);
        }
    }

    private record Source(AlacVirtualWav wav) implements RandomAccessSource {
        @Override
        public long length() {
            return wav.length();
        }

        @Override
        public int read(long position, ByteBuffer target) throws IOException {
            return wav.read(position, target);
        }

        @Override
        public String contentType() {
            return AlacVirtualWav.CONTENT_TYPE;
        }
    }
}
