package com.sun.media.jfxmedia.locator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;

/**
 * cTunes patch of javafx.media (see doc/performance-review.md, section 1, option G).
 * <p>
 * Lets the native player read from a {@link RandomAccessSource}, the same way it reads a local file: random access
 * ("pull mode") through {@link #readBlock}, plus sequential reads through {@link #readNextBlock} and
 * {@link #seek}.
 * <p>
 * The native code copies the bytes from the start of the {@code buffer} field (a direct buffer) after each read,
 * so every read puts its bytes at position 0 of {@code buffer}. It must live in this package: the methods it
 * implements are package-private. Closing the connection does not close the source; its owner does that.
 */
final class StreamConnectionHolder extends ConnectionHolder {
    private final RandomAccessSource source;
    private       long               position;      // for sequential reads

    StreamConnectionHolder(RandomAccessSource source) {
        this.source = source;
        this.channel = new SourceChannel();
    }

    @Override
    int readBlock(long position, int size) throws IOException {
        if (channel == null) {
            throw new ClosedChannelException();
        }
        if (buffer.capacity() < size) {
            buffer = ByteBuffer.allocateDirect(size);
        }
        buffer.rewind()
              .limit(size);
        return source.read(position, buffer);
    }

    @Override
    boolean needBuffer() {
        return false;           // like a local file: no progress buffer in the pipeline
    }

    @Override
    boolean isSeekable() {
        return true;
    }

    @Override
    boolean isRandomAccess() {
        return true;
    }

    @Override
    public long seek(long position) {
        if (position < 0 || position > source.length()) {
            return -1;
        }
        this.position = position;
        return position;
    }

    private final class SourceChannel implements ReadableByteChannel {
        private boolean open = true;

        @Override
        public int read(ByteBuffer target) throws IOException {
            if (!open) {
                throw new ClosedChannelException();
            }
            int read = source.read(position, target);
            if (read > 0) {
                position += read;
            }
            return read;
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void close() {
            open = false;
        }
    }
}
