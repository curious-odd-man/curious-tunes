package com.sun.media.jfxmedia.locator;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * cTunes patch of javafx.media (see doc/performance-review.md, section 1, option G).
 * <p>
 * Media bytes supplied by Java code instead of a file or a URL: any byte range can be read at any time. Registered
 * under a URI in {@link StreamSources}; a {@link Locator} for that URI then plays from it.
 */
public interface RandomAccessSource {
    /**
     * @return the total size in bytes; must not change
     */
    long length();

    /**
     * Copies the bytes from {@code position} into {@code target}, from its current position on, as many as fit and
     * exist. May be called from the player's own threads.
     *
     * @return the number of bytes copied, or -1 if {@code position} is at or past the end
     */
    int read(long position, ByteBuffer target) throws IOException;

    /**
     * @return the MIME type of the bytes, e.g. {@code "audio/x-wav"}
     */
    String contentType();
}
