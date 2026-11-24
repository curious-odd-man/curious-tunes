package com.github.curiousoddman.curious_tunes.domain.audio.alac;

import java.nio.ByteBuffer;

/**
 * The ALAC "magic cookie" ({@code ALACSpecificConfig}, 24 bytes, big-endian): how the packets of a track are encoded.
 * In an MP4 file it is the payload of the {@code alac} box inside the {@code alac} sample entry of {@code stsd}.
 *
 * @param frameLength      frames (samples per channel) in a full packet, normally 4096
 * @param bitDepth         bits per sample: 16, 20, 24 or 32
 * @param historyMult      Rice history multiplier ("pb", normally 40)
 * @param initialHistory   Rice initial history ("mb", normally 10)
 * @param riceLimit        Rice parameter limit ("kb", normally 14)
 * @param channels         channel count
 * @param maxFrameBytes    largest packet, in bytes (0 if unknown)
 * @param sampleRate       frames per second
 */
public record AlacConfig(int frameLength,
                         int bitDepth,
                         int historyMult,
                         int initialHistory,
                         int riceLimit,
                         int channels,
                         int maxFrameBytes,
                         int sampleRate) {
    public static final int SIZE = 24;

    public static AlacConfig parse(ByteBuffer cookie) {
        if (cookie.remaining() < SIZE) {
            throw new AlacException("ALAC config too short: " + cookie.remaining() + " bytes");
        }
        int frameLength = cookie.getInt();
        cookie.get();                                   // compatible version
        int bitDepth       = cookie.get() & 0xFF;
        int historyMult    = cookie.get() & 0xFF;
        int initialHistory = cookie.get() & 0xFF;
        int riceLimit      = cookie.get() & 0xFF;
        int channels       = cookie.get() & 0xFF;
        cookie.getShort();                              // max run
        int maxFrameBytes = cookie.getInt();
        cookie.getInt();                                // average bit rate
        int sampleRate = cookie.getInt();

        if (frameLength <= 0 || frameLength > 1 << 16) {
            throw new AlacException("Unsupported ALAC frame length " + frameLength);
        }
        if (bitDepth != 16 && bitDepth != 20 && bitDepth != 24 && bitDepth != 32) {
            throw new AlacException("Unsupported ALAC bit depth " + bitDepth);
        }
        if (channels < 1 || channels > 8) {
            throw new AlacException("Unsupported ALAC channel count " + channels);
        }
        if (riceLimit == 0 || riceLimit > 32) {
            throw new AlacException("Invalid ALAC Rice limit " + riceLimit);
        }
        return new AlacConfig(frameLength, bitDepth, historyMult, initialHistory, riceLimit, channels,
                maxFrameBytes, sampleRate);
    }
}
