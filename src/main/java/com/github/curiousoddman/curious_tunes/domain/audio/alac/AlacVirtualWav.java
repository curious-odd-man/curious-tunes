package com.github.curiousoddman.curious_tunes.domain.audio.alac;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An ALAC track presented as a WAV file that is never written anywhere: a 44-byte header followed by
 * little-endian PCM, decoded packet by packet as bytes are read (doc/performance-review.md, section 1, option G).
 * <p>
 * Any byte range can be read at any time: a position maps to a frame, the packet index maps the frame to a
 * packet, and only that packet is decoded. The length is known up front, so a player knows the duration at once.
 * A few recently decoded packets are kept, so sequential reads in small blocks decode each packet once.
 * <p>
 * 16-bit tracks become 16-bit WAV; 20- and 24-bit tracks become 24-bit WAV (20-bit samples are shifted up);
 * 32-bit tracks become 32-bit WAV. Thread-safe: reads are serialised.
 */
public final class AlacVirtualWav implements Closeable {
    public static final  int HEADER_SIZE    = 44;
    private static final int CACHED_PACKETS = 16;

    private final FileChannel          file;
    private final Mp4AlacTrack         track;
    private final AlacPacketDecoder    decoder;
    private final int                  channels;
    private final int                  bytesPerSample;
    private final int                  blockAlign;
    private final int                  sampleShift;      // 20-bit samples are written as 24-bit
    private final byte[]               header;
    private final long                 length;
    private final int[][]              samples;
    private final ByteBuffer           packetBuffer;
    private final Map<Integer, byte[]> decoded = new LinkedHashMap<>(CACHED_PACKETS * 2, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, byte[]> eldest) {
            return size() > CACHED_PACKETS;
        }
    };

    private AlacVirtualWav(FileChannel file, Mp4AlacTrack track) {
        this.file = file;
        this.track = track;
        AlacConfig config = track.config();
        if (config.channels() > 2) {
            throw new AlacException("Only mono and stereo ALAC are supported, not " + config.channels() + " channels");
        }
        decoder = new AlacPacketDecoder(config);
        channels = config.channels();
        bytesPerSample = switch (config.bitDepth()) {
            case 16 -> 2;
            case 20, 24 -> 3;
            default -> 4;
        };
        sampleShift = config.bitDepth() == 20 ? 4 : 0;
        blockAlign = channels * bytesPerSample;
        length = HEADER_SIZE + track.totalFrames() * blockAlign;
        header = wavHeader(config.sampleRate(), channels, bytesPerSample * 8, track.totalFrames() * blockAlign);
        samples = new int[channels][config.frameLength()];
        packetBuffer = ByteBuffer.allocate(Math.max(1, track.maxPacketSize()));
    }

    /**
     * Opens the file and reads its packet index (the audio itself is read as it's needed).
     *
     * @throws AlacException if it is not an ALAC file this class can decode
     */
    public static AlacVirtualWav open(Path file) throws IOException {
        FileChannel channel = FileChannel.open(file, StandardOpenOption.READ);
        try {
            return new AlacVirtualWav(channel, Mp4AlacTrack.read(channel));
        } catch (IOException | RuntimeException e) {
            channel.close();
            throw e;
        }
    }

    public static final String CONTENT_TYPE = "audio/x-wav";

    /**
     * @return the size of the whole virtual WAV file, header included
     */
    public long length() {
        return length;
    }

    public int sampleRate() {
        return track.config()
                    .sampleRate();
    }

    public int channels() {
        return channels;
    }

    public int bitsPerSample() {
        return bytesPerSample * 8;
    }

    public long totalFrames() {
        return track.totalFrames();
    }

    /**
     * Copies the bytes from {@code position} into {@code target}, as many as fit and exist.
     *
     * @return the number of bytes copied, or -1 if {@code position} is at or past the end
     */
    public synchronized int read(long position, ByteBuffer target) throws IOException {
        if (position < 0 || position >= length) {
            return -1;
        }
        int  start = target.position();
        long at    = position;
        while (target.hasRemaining() && at < length) {
            if (at < HEADER_SIZE) {
                int count = (int) Math.min(HEADER_SIZE - at, target.remaining());
                target.put(header, (int) at, count);
                at += count;
                continue;
            }
            long   dataOffset  = at - HEADER_SIZE;
            long   frame       = dataOffset / blockAlign;
            int    packet      = track.packetForFrame(frame);
            byte[] pcm         = packetPcm(packet);
            long   packetStart = track.packetFirstFrame(packet) * blockAlign;
            int    offset      = (int) (dataOffset - packetStart);
            int    count       = Math.min(pcm.length - offset, target.remaining());
            if (count <= 0) {
                break;                                  // cannot happen with a consistent index
            }
            target.put(pcm, offset, count);
            at += count;
        }
        return target.position() - start;
    }

    @Override
    public synchronized void close() throws IOException {
        decoded.clear();
        file.close();
    }

    /**
     * The PCM bytes of one packet, exactly as long as the index says the packet is (shorter decoded packets are
     * padded with silence, longer ones cut), so byte positions always line up with the index.
     */
    private byte[] packetPcm(int packet) throws IOException {
        byte[] cached = decoded.get(packet);
        if (cached != null) {
            return cached;
        }
        int size = track.packetSize(packet);
        packetBuffer.clear()
                    .limit(size);
        long at = track.packetOffset(packet);
        while (packetBuffer.hasRemaining()) {
            if (file.read(packetBuffer, at + packetBuffer.position()) < 0) {
                throw new AlacException("File ends inside packet " + packet);
            }
        }
        int    frames   = decoder.decode(packetBuffer.array(), size, samples);
        int    expected = track.packetFrames(packet);
        byte[] pcm      = new byte[expected * blockAlign];
        int    usable   = Math.min(frames, expected);
        int    p        = 0;
        for (int i = 0; i < usable; i++) {
            for (int ch = 0; ch < channels; ch++) {
                int value = samples[ch][i] << sampleShift;
                pcm[p++] = (byte) value;
                pcm[p++] = (byte) (value >> 8);
                if (bytesPerSample >= 3) {
                    pcm[p++] = (byte) (value >> 16);
                }
                if (bytesPerSample == 4) {
                    pcm[p++] = (byte) (value >> 24);
                }
            }
        }
        decoded.put(packet, pcm);
        return pcm;
    }

    private static byte[] wavHeader(int sampleRate, int channels, int bitsPerSample, long dataBytes) {
        int  blockAlign = channels * bitsPerSample / 8;
        long riffSize   = Math.min(0xFFFFFFFFL, 36 + dataBytes);
        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE)
                                      .order(ByteOrder.LITTLE_ENDIAN);
        header.put(new byte[]{'R', 'I', 'F', 'F'})
              .putInt((int) riffSize)
              .put(new byte[]{'W', 'A', 'V', 'E'})
              .put(new byte[]{'f', 'm', 't', ' '})
              .putInt(16)
              .putShort((short) 1)                       // PCM
              .putShort((short) channels)
              .putInt(sampleRate)
              .putInt(sampleRate * blockAlign)
              .putShort((short) blockAlign)
              .putShort((short) bitsPerSample)
              .put(new byte[]{'d', 'a', 't', 'a'})
              .putInt((int) Math.min(0xFFFFFFFFL, dataBytes));
        return header.array();
    }
}
