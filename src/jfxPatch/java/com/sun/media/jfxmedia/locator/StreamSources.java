package com.sun.media.jfxmedia.locator;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * cTunes patch of javafx.media (see doc/performance-review.md, section 1, option G).
 * <p>
 * The {@link RandomAccessSource}s the patched {@link Locator} plays from, by URI. Register a source, then create a
 * {@code javafx.scene.media.Media} for the same URI; unregister when the player is disposed. Use URIs that never
 * name a real file, e.g. {@code file:///ctunes-stream/123-1.wav}: the {@code file} scheme passes {@code Media}'s
 * protocol check, and the patched {@code Locator} answers before anything looks at the disk.
 */
public final class StreamSources {
    /**
     * The JavaFX version the patch was made for. {@link Locator#CTUNES_STREAM_PATCH} must be the same, which shows
     * that the patched {@code Locator}, not JavaFX's own, is the one in use.
     */
    public static final String PATCH_VERSION = "25.0.1";

    private static final Map<String, RandomAccessSource> SOURCES = new ConcurrentHashMap<>();

    private StreamSources() {
    }

    public static void register(URI uri, RandomAccessSource source) {
        SOURCES.put(key(uri), source);
    }

    public static void unregister(URI uri) {
        SOURCES.remove(key(uri));
    }

    /**
     * @return the source registered for {@code uri}, or null
     */
    public static RandomAccessSource lookup(URI uri) {
        return uri == null ? null : SOURCES.get(key(uri));
    }

    /**
     * True when this class and the {@code Locator} in use come from the same patch: the patch is active.
     */
    public static boolean isPatchActive() {
        try {
            Object version = Locator.class.getField("CTUNES_STREAM_PATCH")
                                          .get(null);
            return PATCH_VERSION.equals(version) && Locator.class.getModule() == StreamSources.class.getModule();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return false;
        }
    }

    private static String key(URI uri) {
        return uri.normalize()
                  .toASCIIString();
    }
}
