package com.github.curiousoddman.curious_tunes.domain.audio.alac;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The test files were made with FFmpeg ({@code -c:a alac}); the expected checksums are of FFmpeg's own decoding of
 * them ({@code ffmpeg -i <file> -f s16le|s24le - | md5sum}), so the decoder is checked bit for bit against an
 * independent implementation. {@code stereo16.m4a} contains a sweep, digital silence (zero runs), loud noise
 * and tones; {@code mono24.m4a} is a 24-bit chirp with noise. {@code noise16.m4a} (stereo) and {@code noise24.m4a}
 * (mono) are full-scale white noise, which cannot be compressed, so every packet in them is an uncompressed
 * ("escape") packet.
 */
class AlacVirtualWavTest {
    private static final Path STEREO16 = resource("stereo16.m4a");
    private static final Path MONO24   = resource("mono24.m4a");

    @Test
    void stereo16DecodesExactlyLikeFfmpeg() throws Exception {
        try (AlacVirtualWav wav = AlacVirtualWav.open(STEREO16)) {
            assertEquals(44100, wav.sampleRate());
            assertEquals(2, wav.channels());
            assertEquals(16, wav.bitsPerSample());
            assertEquals(74970, wav.totalFrames());
            byte[] pcm = readAll(wav, 4096);
            assertEquals(299880, pcm.length - AlacVirtualWav.HEADER_SIZE);
            assertEquals("758803a071f352f9c0665b6dff992119", md5(pcm, AlacVirtualWav.HEADER_SIZE));
        }
    }

    @Test
    void mono24DecodesExactlyLikeFfmpeg() throws Exception {
        try (AlacVirtualWav wav = AlacVirtualWav.open(MONO24)) {
            assertEquals(48000, wav.sampleRate());
            assertEquals(1, wav.channels());
            assertEquals(24, wav.bitsPerSample());
            byte[] pcm = readAll(wav, 4096);
            assertEquals(158400, pcm.length - AlacVirtualWav.HEADER_SIZE);
            assertEquals("bb41e3bc141e72ba7d9b5bfc05913f20", md5(pcm, AlacVirtualWav.HEADER_SIZE));
        }
    }

    @Test
    void uncompressedPacketsDecodeExactlyLikeFfmpeg() throws Exception {
        try (AlacVirtualWav wav = AlacVirtualWav.open(resource("noise16.m4a"))) {
            byte[] pcm = readAll(wav, 1000);
            assertEquals(52920, pcm.length - AlacVirtualWav.HEADER_SIZE);
            assertEquals("f6338e1582dfb3483f87bd425fa35384", md5(pcm, AlacVirtualWav.HEADER_SIZE));
        }
        try (AlacVirtualWav wav = AlacVirtualWav.open(resource("noise24.m4a"))) {
            byte[] pcm = readAll(wav, 1000);
            assertEquals(39690, pcm.length - AlacVirtualWav.HEADER_SIZE);
            assertEquals("d8d5a4bc7cec94540d4521dccd4c1f7a", md5(pcm, AlacVirtualWav.HEADER_SIZE));
        }
    }

    @Test
    void headerDescribesThePcm() throws Exception {
        try (AlacVirtualWav wav = AlacVirtualWav.open(STEREO16)) {
            ByteBuffer header = ByteBuffer.allocate(AlacVirtualWav.HEADER_SIZE);
            wav.read(0, header);
            header.flip()
                  .order(ByteOrder.LITTLE_ENDIAN);
            assertEquals("RIFF", ascii(header, 0));
            assertEquals("WAVE", ascii(header, 8));
            assertEquals(1, header.getShort(20));            // PCM
            assertEquals(2, header.getShort(22));
            assertEquals(44100, header.getInt(24));
            assertEquals(4, header.getShort(32));            // block align
            assertEquals(16, header.getShort(34));
            assertEquals("data", ascii(header, 36));
            assertEquals(wav.length() - AlacVirtualWav.HEADER_SIZE, header.getInt(40));
        }
    }

    @Test
    void randomReadsReturnTheSameBytesAsASequentialRead() throws Exception {
        byte[] expected;
        try (AlacVirtualWav wav = AlacVirtualWav.open(STEREO16)) {
            expected = readAll(wav, 4096);
        }
        try (AlacVirtualWav wav = AlacVirtualWav.open(STEREO16)) {
            Random random = new Random(7);
            for (int i = 0; i < 300; i++) {
                long       position      = random.nextInt(expected.length);
                int        size          = 1 + random.nextInt(20_000);
                ByteBuffer target        = ByteBuffer.allocate(size);
                int        read          = wav.read(position, target);
                int        expectedCount = (int) Math.min(size, expected.length - position);
                assertEquals(expectedCount, read, "at " + position);
                assertArrayEquals(Arrays.copyOfRange(expected, (int) position, (int) position + read),
                        Arrays.copyOf(target.array(), read), "at " + position);
            }
            assertEquals(-1, wav.read(wav.length(), ByteBuffer.allocate(10)));
        }
    }

    private static byte[] readAll(AlacVirtualWav wav, int blockSize) throws Exception {
        ByteArrayOutputStream out      = new ByteArrayOutputStream();
        long                  position = 0;
        ByteBuffer            block    = ByteBuffer.allocate(blockSize);
        int                   read;
        while ((read = wav.read(position, block.clear())) > 0) {
            out.write(block.array(), 0, read);
            position += read;
        }
        return out.toByteArray();
    }

    private static String md5(byte[] bytes, int from) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(bytes, from, bytes.length - from);
        return HexFormat.of()
                        .formatHex(digest.digest());
    }

    private static String ascii(ByteBuffer buffer, int at) {
        return new String(new byte[]{buffer.get(at), buffer.get(at + 1), buffer.get(at + 2), buffer.get(at + 3)});
    }

    private static Path resource(String name) {
        try {
            return Path.of(AlacVirtualWavTest.class.getResource("/alac/" + name)
                                                   .toURI());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
