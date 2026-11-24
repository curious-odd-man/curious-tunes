package com.github.curiousoddman.curious_tunes.domain.audio.alac;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The ALAC audio track of an MP4 ({@code .m4a}) file: its config and an index of its packets — where each one is in
 * the file, how long it is, and which frame it starts at. Built from the sample tables
 * ({@code moov/trak/mdia/minf/stbl}): {@code stsd} (config), {@code stsz} (sizes), {@code stco}/{@code co64}
 * (chunk offsets), {@code stsc} (packets per chunk) and {@code stts} (frames per packet).
 * <p>
 * Only the {@code moov} box is read (a few KB to a few hundred KB); the audio data itself is not touched.
 */
public final class Mp4AlacTrack {
    private static final long MAX_MOOV_BYTES = 64L * 1024 * 1024;

    private final AlacConfig config;
    private final long[]     packetOffset;
    private final int[]      packetSize;
    private final long[]     packetFirstFrame;      // one more entry than packets: the last is the total
    private final int        maxPacketSize;

    private Mp4AlacTrack(AlacConfig config, long[] packetOffset, int[] packetSize, long[] packetFirstFrame) {
        this.config = config;
        this.packetOffset = packetOffset;
        this.packetSize = packetSize;
        this.packetFirstFrame = packetFirstFrame;
        int max = 0;
        for (int size : packetSize) {
            max = Math.max(max, size);
        }
        this.maxPacketSize = max;
    }

    public AlacConfig config() {
        return config;
    }

    public int packetCount() {
        return packetSize.length;
    }

    public long packetOffset(int packet) {
        return packetOffset[packet];
    }

    public int packetSize(int packet) {
        return packetSize[packet];
    }

    public long packetFirstFrame(int packet) {
        return packetFirstFrame[packet];
    }

    public int packetFrames(int packet) {
        return (int) (packetFirstFrame[packet + 1] - packetFirstFrame[packet]);
    }

    public long totalFrames() {
        return packetFirstFrame[packetSize.length];
    }

    public int maxPacketSize() {
        return maxPacketSize;
    }

    /**
     * @return the packet that contains {@code frame} (0-based), clamped to the last packet
     */
    public int packetForFrame(long frame) {
        int index = Arrays.binarySearch(packetFirstFrame, 0, packetSize.length, frame);
        int packet = index >= 0 ? index : -index - 2;
        return Math.max(0, Math.min(packetSize.length - 1, packet));
    }

    /**
     * Reads the index from the file.
     *
     * @throws AlacException if the file has no ALAC audio track, or its tables are inconsistent
     */
    public static Mp4AlacTrack read(FileChannel file) throws IOException {
        ByteBuffer moov = readTopLevelBox(file, "moov");
        if (moov == null) {
            throw new AlacException("No moov box");
        }
        for (ByteBuffer trak : children(moov, "trak")) {
            ByteBuffer mdia = child(trak, "mdia");
            if (mdia == null || !"soun".equals(handlerType(mdia))) {
                continue;
            }
            ByteBuffer stbl = path(mdia, "minf", "stbl");
            if (stbl == null) {
                continue;
            }
            AlacConfig config = alacConfig(stbl);
            if (config == null) {
                continue;                               // a sound track, but not ALAC
            }
            return build(config, stbl, timescale(mdia));
        }
        throw new AlacException("No ALAC audio track");
    }

    private static Mp4AlacTrack build(AlacConfig config, ByteBuffer stbl, long timescale) {
        int[]  sizes        = sampleSizes(require(stbl, "stsz"));
        long[] chunkOffsets = chunkOffsets(stbl);
        int[]  stsc         = fullBoxEntries(require(stbl, "stsc"), 3);
        long[] durations    = timeToSample(require(stbl, "stts"), sizes.length);

        long[] offsets = new long[sizes.length];
        int    packet  = 0;
        for (int chunk = 0; chunk < chunkOffsets.length && packet < sizes.length; chunk++) {
            int  perChunk = packetsInChunk(stsc, chunk + 1);
            long offset   = chunkOffsets[chunk];
            for (int i = 0; i < perChunk && packet < sizes.length; i++) {
                offsets[packet] = offset;
                offset += sizes[packet];
                packet++;
            }
        }
        if (packet != sizes.length) {
            throw new AlacException("Chunk tables cover " + packet + " of " + sizes.length + " packets");
        }
        // Durations are in the track's timescale, which for audio is normally the sample rate.
        boolean scale      = timescale > 0 && timescale != config.sampleRate();
        long[]  firstFrame = new long[sizes.length + 1];
        long    time       = 0;
        for (int i = 0; i < sizes.length; i++) {
            time += durations[i];
            firstFrame[i + 1] = scale ? Math.round((double) time * config.sampleRate() / timescale) : time;
        }
        return new Mp4AlacTrack(config, offsets, sizes, firstFrame);
    }

    // ---- boxes -------------------------------------------------------------------------------------------------

    private static ByteBuffer readTopLevelBox(FileChannel file, String type) throws IOException {
        long       position = 0;
        long       size     = file.size();
        ByteBuffer header   = ByteBuffer.allocate(16);
        while (position + 8 <= size) {
            header.clear()
                  .limit(8);
            file.read(header, position);
            header.flip();
            long   boxSize   = header.getInt() & 0xFFFFFFFFL;
            String boxType   = fourCc(header.getInt());
            int    headerLen = 8;
            if (boxSize == 1) {
                header.clear()
                      .limit(8);
                file.read(header, position + 8);
                header.flip();
                boxSize = header.getLong();
                headerLen = 16;
            } else if (boxSize == 0) {
                boxSize = size - position;
            }
            if (boxSize < headerLen) {
                throw new AlacException("Invalid box size at " + position);
            }
            if (boxType.equals(type)) {
                long contentSize = boxSize - headerLen;
                if (contentSize > MAX_MOOV_BYTES) {
                    throw new AlacException("moov box too large: " + contentSize);
                }
                ByteBuffer content = ByteBuffer.allocate((int) contentSize);
                long       at      = position + headerLen;
                while (content.hasRemaining()) {
                    int read = file.read(content, at + content.position());
                    if (read < 0) {
                        throw new AlacException("File ends inside the moov box");
                    }
                }
                return content.flip()
                              .order(ByteOrder.BIG_ENDIAN);
            }
            position += boxSize;
        }
        return null;
    }

    /**
     * The direct children of a box's content with the given type, each as a buffer over its own content.
     */
    private static List<ByteBuffer> children(ByteBuffer parent, String type) {
        List<ByteBuffer> result = new ArrayList<>();
        int              at     = 0;
        while (at + 8 <= parent.limit()) {
            long   size      = parent.getInt(at) & 0xFFFFFFFFL;
            String boxType   = fourCc(parent.getInt(at + 4));
            int    headerLen = 8;
            if (size == 1) {
                size = parent.getLong(at + 8);
                headerLen = 16;
            } else if (size == 0) {
                size = parent.limit() - at;
            }
            if (size < headerLen || at + size > parent.limit()) {
                break;                                  // damaged or padding: stop here
            }
            if (boxType.equals(type)) {
                result.add(parent.slice(at + headerLen, (int) size - headerLen)
                                 .order(ByteOrder.BIG_ENDIAN));
            }
            at += (int) size;
        }
        return result;
    }

    private static ByteBuffer child(ByteBuffer parent, String type) {
        List<ByteBuffer> found = children(parent, type);
        return found.isEmpty() ? null : found.getFirst();
    }

    private static ByteBuffer path(ByteBuffer parent, String... types) {
        ByteBuffer box = parent;
        for (String type : types) {
            box = child(box, type);
            if (box == null) {
                return null;
            }
        }
        return box;
    }

    private static ByteBuffer require(ByteBuffer parent, String type) {
        ByteBuffer box = child(parent, type);
        if (box == null) {
            throw new AlacException("Missing " + type + " box");
        }
        return box;
    }

    private static long timescale(ByteBuffer mdia) {
        ByteBuffer mdhd = child(mdia, "mdhd");
        if (mdhd == null || mdhd.limit() < 16) {
            return 0;
        }
        int version = mdhd.get(0) & 0xFF;
        int at      = version == 1 ? 20 : 12;           // after version/flags and the creation/modification times
        return mdhd.limit() < at + 4 ? 0 : mdhd.getInt(at) & 0xFFFFFFFFL;
    }

    private static String handlerType(ByteBuffer mdia) {
        ByteBuffer hdlr = child(mdia, "hdlr");
        return hdlr == null || hdlr.limit() < 12 ? null : fourCc(hdlr.getInt(8));     // after version/flags and pre_defined
    }

    /**
     * The config inside the first {@code alac} sample entry of {@code stsd}, or null if the entry is not ALAC.
     */
    private static AlacConfig alacConfig(ByteBuffer stbl) {
        ByteBuffer stsd = require(stbl, "stsd");
        if (stsd.limit() < 16) {
            return null;
        }
        // stsd: version/flags (4), entry count (4), then sample entries (boxes).
        ByteBuffer entries = stsd.slice(8, stsd.limit() - 8);
        ByteBuffer entry   = child(entries, "alac");
        if (entry == null) {
            return null;
        }
        // An audio sample entry starts with 28 bytes of fixed fields; the 'alac' config box follows (in QuickTime
        // version 1 entries 16 bytes later). Look for it rather than assume the version.
        for (int at = 28; at + 12 + AlacConfig.SIZE <= entry.limit(); at++) {
            long size = entry.getInt(at) & 0xFFFFFFFFL;
            if (size >= 12 + AlacConfig.SIZE && fourCc(entry.getInt(at + 4)).equals("alac")) {
                return AlacConfig.parse(entry.slice(at + 12, AlacConfig.SIZE)
                                             .order(ByteOrder.BIG_ENDIAN));
            }
        }
        throw new AlacException("ALAC sample entry without its config");
    }

    private static int[] sampleSizes(ByteBuffer stsz) {
        int uniformSize = stsz.getInt(4);
        int count       = stsz.getInt(8);
        if (count < 0) {
            throw new AlacException("Invalid packet count");
        }
        int[] sizes = new int[count];
        if (uniformSize != 0) {
            Arrays.fill(sizes, uniformSize);
        } else {
            if (stsz.limit() < 12 + 4L * count) {
                throw new AlacException("stsz box too short");
            }
            for (int i = 0; i < count; i++) {
                sizes[i] = stsz.getInt(12 + 4 * i);
            }
        }
        return sizes;
    }

    private static long[] chunkOffsets(ByteBuffer stbl) {
        ByteBuffer stco = child(stbl, "stco");
        if (stco != null) {
            int    count   = stco.getInt(4);
            long[] offsets = new long[count];
            for (int i = 0; i < count; i++) {
                offsets[i] = stco.getInt(8 + 4 * i) & 0xFFFFFFFFL;
            }
            return offsets;
        }
        ByteBuffer co64    = require(stbl, "co64");
        int        count   = co64.getInt(4);
        long[]     offsets = new long[count];
        for (int i = 0; i < count; i++) {
            offsets[i] = co64.getLong(8 + 8 * i);
        }
        return offsets;
    }

    /**
     * Entries of a full box with an entry count, {@code fields} 32-bit values each, flattened.
     */
    private static int[] fullBoxEntries(ByteBuffer box, int fields) {
        int count = box.getInt(4);
        if (count < 0 || box.limit() < 8 + 4L * fields * count) {
            throw new AlacException("Table too short");
        }
        int[] values = new int[count * fields];
        for (int i = 0; i < values.length; i++) {
            values[i] = box.getInt(8 + 4 * i);
        }
        return values;
    }

    /**
     * @param stsc  (first chunk, packets per chunk, description index) triples
     * @param chunk 1-based
     */
    private static int packetsInChunk(int[] stsc, int chunk) {
        int perChunk = 0;
        for (int i = 0; i < stsc.length; i += 3) {
            if (stsc[i] > chunk) {
                break;
            }
            perChunk = stsc[i + 1];
        }
        return perChunk;
    }

    private static long[] timeToSample(ByteBuffer stts, int packets) {
        int[]  entries   = fullBoxEntries(stts, 2);
        long[] durations = new long[packets];
        int    packet    = 0;
        for (int i = 0; i < entries.length && packet < packets; i += 2) {
            for (int n = 0; n < entries[i] && packet < packets; n++) {
                durations[packet++] = entries[i + 1] & 0xFFFFFFFFL;
            }
        }
        if (packet != packets) {
            throw new AlacException("stts covers " + packet + " of " + packets + " packets");
        }
        return durations;
    }

    private static String fourCc(int value) {
        return new String(new char[]{
                (char) ((value >>> 24) & 0xFF), (char) ((value >>> 16) & 0xFF),
                (char) ((value >>> 8) & 0xFF), (char) (value & 0xFF)});
    }
}
