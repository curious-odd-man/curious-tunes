package com.github.curiousoddman.curious_tunes.util;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Screen;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Album covers for display (doc/performance-review.md, item 2.1).
 * <ul>
 *     <li>Decoded at the size they are shown at, rounded up to a few steps ({@link #SIZES}) and multiplied by the
 *     screen's scale, instead of at the full size of the embedded cover (often 1000–3000 px).</li>
 *     <li>Decoded once per cover and size: a cache, least recently used first out, capped by the memory the
 *     pixels take ({@link #MAX_CACHE_BYTES}).</li>
 *     <li>Decoded off the JavaFX thread: the view shows the shared placeholder (or what it showed before) until the
 *     cover is ready.</li>
 *     <li>A view that is reused for another album (a list cell, a reloaded row) never ends up with the cover it
 *     asked for earlier.</li>
 * </ul>
 * Covers are recognised by their bytes, not only by album, so a cover changed in the tag editor shows the new
 * image without the cache having to be told.
 * <p>
 * When the album is known and the cover is shown at up to 512 px, the picture is decoded from a small stored
 * thumbnail ({@link ThumbnailSource}) instead of from the full-size cover.
 */
@Slf4j
public final class CoverImages {
    static final int[] SIZES           = {64, 128, 256, 512, 1024};
    static final long  MAX_CACHE_BYTES = 128L * 1024 * 1024;

    private static final String NO_IMAGE = "/img/noimage.png";
    private static final String VIEW_KEY = CoverImages.class.getName() + ".key";

    record Key(int size, int length, long fingerprint) {
    }

    private static final Map<Key, Image> CACHE = new LinkedHashMap<>(64, 0.75f, true);   // access order: LRU
    private static       long            cachedBytes;

    /**
     * Two decoders are enough: covers are small at display size, and this keeps a burst of new rows from
     * starting hundreds of decodes at once.
     */
    private static final ExecutorService DECODER = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "cover-decoder");
        thread.setDaemon(true);
        return thread;
    });

    private static volatile double          screenScale = 1;
    private static volatile Image           placeholder;
    private static volatile ThumbnailSource thumbnails;

    /**
     * Supplies small JPEG copies of covers (doc/performance-review.md, item 2.1). Called on the decoder threads.
     */
    public interface ThumbnailSource {
        /**
         * @param px the thumbnail size: 128 or 512
         * @return the thumbnail's bytes, or the cover itself when no thumbnail can be made
         */
        byte[] thumbnail(int albumId, byte[] cover, int px);
    }

    public static void setThumbnailSource(ThumbnailSource source) {
        thumbnails = source;
    }

    private CoverImages() {
    }

    /**
     * Shows the cover in {@code view}, decoded for {@code displaySize} (the view's size in layout pixels). Does
     * nothing when there is no cover, so the view keeps its placeholder. Safe to call from any thread.
     */
    public static void show(ImageView view, byte[] cover, double displaySize) {
        show(view, null, cover, displaySize);
    }

    /**
     * Like {@link #show(ImageView, byte[], double)}; with the album known, a small stored thumbnail is decoded
     * instead of the full-size cover where it is enough.
     */
    public static void show(ImageView view, Integer albumId, byte[] cover, double displaySize) {
        if (cover == null || cover.length == 0) {
            return;
        }
        Key key = key(cover, pixelSize(displaySize));
        view.getProperties()
            .put(VIEW_KEY, key);
        Image cached = cached(key);
        if (cached != null) {
            setOnFxThread(view, key, cached);
            return;
        }
        if (view.getImage() == null) {
            view.setImage(placeholder());
        }
        DECODER.execute(() -> {
            Image image = decode(source(albumId, cover, key.size()), key.size());
            if (image == null) {
                return;
            }
            store(key, image);
            setOnFxThread(view, key, image);
        });
    }

    /**
     * Decodes now, on the calling thread, for {@code displaySize}; the cache is used. For the odd single image
     * (a cover being edited); lists use {@link #show}.
     *
     * @return null if the bytes are not an image
     */
    public static Image get(byte[] cover, double displaySize) {
        if (cover == null || cover.length == 0) {
            return null;
        }
        Key   key    = key(cover, pixelSize(displaySize));
        Image cached = cached(key);
        if (cached != null) {
            return cached;
        }
        Image image = decode(cover, key.size());
        if (image != null) {
            store(key, image);
        }
        return image;
    }

    /**
     * The "no cover" picture, loaded once for the whole app.
     */
    public static Image placeholder() {
        Image image = placeholder;
        if (image == null) {
            image = new Image(Objects.requireNonNull(CoverImages.class.getResource(NO_IMAGE))
                                     .toExternalForm());
            placeholder = image;
        }
        return image;
    }

    /**
     * The smallest step at least {@code pixels} wide (the largest step for anything bigger).
     */
    static int bucket(double pixels) {
        for (int size : SIZES) {
            if (size >= pixels) {
                return size;
            }
        }
        return SIZES[SIZES.length - 1];
    }

    /**
     * Cheap identity for cover bytes: the length plus a hash of up to 512 bytes spread over the whole array.
     * Different covers practically never share both; hashing every byte of a large cover on every lookup would
     * cost more than the cache saves.
     */
    static long fingerprint(byte[] bytes) {
        long hash   = 1125899906842597L;
        int  step   = Math.max(1, bytes.length / 512);
        for (int i = 0; i < bytes.length; i += step) {
            hash = 31 * hash + bytes[i];
        }
        return 31 * hash + bytes[bytes.length - 1];
    }

    private static int pixelSize(double displaySize) {
        if (Platform.isFxApplicationThread()) {
            screenScale = Screen.getPrimary()
                                .getOutputScaleX();      // remembered for calls from other threads
        }
        return bucket((displaySize > 0 ? displaySize : 256) * screenScale);
    }

    /**
     * The bytes to decode for {@code size}: a thumbnail when one is enough, else the cover itself.
     */
    private static byte[] source(Integer albumId, byte[] cover, int size) {
        ThumbnailSource source = thumbnails;
        int             px     = size <= 128 ? 128 : size <= 512 ? 512 : 0;
        if (albumId == null || source == null || px == 0) {
            return cover;
        }
        try {
            byte[] thumbnail = source.thumbnail(albumId, cover, px);
            return thumbnail == null ? cover : thumbnail;
        } catch (RuntimeException e) {
            log.warn("No thumbnail for album {}", albumId, e);
            return cover;
        }
    }

    private static Key key(byte[] cover, int size) {
        return new Key(size, cover.length, fingerprint(cover));
    }

    private static Image decode(byte[] cover, int size) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(cover)) {
            Image image = new Image(in, size, size, true, true);
            return image.isError() ? null : image;
        } catch (Exception e) {
            log.warn("Unable to decode an album cover", e);
            return null;
        }
    }

    private static Image cached(Key key) {
        synchronized (CACHE) {
            return CACHE.get(key);
        }
    }

    private static void store(Key key, Image image) {
        synchronized (CACHE) {
            if (CACHE.put(key, image) == null) {
                cachedBytes += weight(image);
            }
            for (Iterator<Image> it = CACHE.values()
                                          .iterator(); cachedBytes > MAX_CACHE_BYTES && it.hasNext(); ) {
                Image eldest = it.next();
                if (eldest == image) {
                    continue;
                }
                cachedBytes -= weight(eldest);
                it.remove();
            }
        }
    }

    private static long weight(Image image) {
        return (long) image.getWidth() * (long) image.getHeight() * 4;
    }

    private static void setOnFxThread(ImageView view, Key key, Image image) {
        Runnable apply = () -> {
            if (key.equals(view.getProperties()
                               .get(VIEW_KEY))) {     // the view hasn't been given another cover since
                view.setImage(image);
            }
        };
        if (Platform.isFxApplicationThread()) {
            apply.run();
        } else {
            Platform.runLater(apply);
        }
    }
}
