package com.github.curiousoddman.curious_tunes.domain.audio.alac;

/**
 * Decodes one ALAC packet at a time into PCM samples. Packets are independent of each other, which is what makes
 * decoding from any packet (seeking) possible.
 * <p>
 * Follows the ALAC format as published by Apple (the ALAC reference decoder, Apache 2.0) and as implemented by
 * FFmpeg's {@code alac.c}: per element a small header, an optional stereo decorrelation, an adaptive-Golomb (Rice)
 * coded prediction error, and an adaptive LPC predictor; or, for an "escape" element, uncompressed samples.
 * Supports mono and stereo (SCE/CPE elements, plus LFE), 16/20/24/32-bit. Not thread-safe: one decoder per
 * reader.
 */
public final class AlacPacketDecoder {
    private static final int TYPE_SCE = 0;
    private static final int TYPE_CPE = 1;
    private static final int TYPE_LFE = 3;
    private static final int TYPE_END = 7;

    private final AlacConfig config;
    private final BitReader  bits = new BitReader();
    private final int[][]    predictionError;
    private final int[][]    extraBits;
    private final int[][]    lpcCoefs      = new int[2][32];
    private final int[]      lpcOrder      = new int[2];
    private final int[]      lpcQuant      = new int[2];
    private final int[]      predictionType = new int[2];
    private final int[]      riceMult      = new int[2];

    public AlacPacketDecoder(AlacConfig config) {
        this.config = config;
        predictionError = new int[2][config.frameLength()];
        extraBits = new int[2][config.frameLength()];
    }

    public AlacConfig getConfig() {
        return config;
    }

    /**
     * @param packet bytes of one packet
     * @param length how many bytes of {@code packet} belong to it
     * @param out    one array per channel, each at least {@link AlacConfig#frameLength()} long; receives the
     *               samples as signed values of {@link AlacConfig#bitDepth()} bits
     * @return the number of frames (samples per channel) in the packet
     */
    public int decode(byte[] packet, int length, int[][] out) {
        bits.reset(packet, length);
        int channel = 0;
        int frames  = -1;
        while (bits.bitsLeft() >= 3) {
            int element = bits.readBits(3);
            if (element == TYPE_END) {
                break;
            }
            if (element != TYPE_SCE && element != TYPE_CPE && element != TYPE_LFE) {
                throw new AlacException("Unsupported ALAC element type " + element);
            }
            int channels = element == TYPE_CPE ? 2 : 1;
            if (channel + channels > config.channels()) {
                throw new AlacException("More channels in the packet than in the config");
            }
            int count = decodeElement(channels, out, channel);
            if (frames >= 0 && count != frames) {
                throw new AlacException("Elements of one packet disagree on the frame count");
            }
            frames = count;
            channel += channels;
        }
        if (channel != config.channels() || frames < 0) {
            throw new AlacException("Packet has " + channel + " channels, expected " + config.channels());
        }
        return frames;
    }

    private int decodeElement(int channels, int[][] out, int firstChannel) {
        bits.skip(4);                                   // element instance tag
        bits.skip(12);                                  // unused header bits
        boolean hasSize      = bits.readBit() == 1;
        int     extra        = bits.readBits(2) << 3;   // bytes shifted out of each sample, as bits
        boolean isCompressed = bits.readBit() == 0;
        int     bps          = config.bitDepth() - extra + channels - 1;
        if (bps > 32 || bps < 1) {
            throw new AlacException("Invalid ALAC sample size " + bps);
        }
        int frames = hasSize ? bits.readBits(32) : config.frameLength();
        if (frames <= 0 || frames > config.frameLength()) {
            throw new AlacException("Invalid ALAC frame count " + Integer.toUnsignedString(frames));
        }

        int decorrShift      = 0;
        int decorrLeftWeight = 0;
        if (isCompressed) {
            decorrShift = bits.readBits(8);
            decorrLeftWeight = bits.readBits(8);
            for (int ch = 0; ch < channels; ch++) {
                predictionType[ch] = bits.readBits(4);
                lpcQuant[ch] = bits.readBits(4);
                riceMult[ch] = bits.readBits(3);
                lpcOrder[ch] = bits.readBits(5);
                if (lpcQuant[ch] == 0) {
                    throw new AlacException("Invalid ALAC LPC quantisation 0");
                }
                for (int i = lpcOrder[ch] - 1; i >= 0; i--) {
                    lpcCoefs[ch][i] = bits.readSigned(16);
                }
            }
            if (extra > 0) {
                for (int i = 0; i < frames; i++) {
                    for (int ch = 0; ch < channels; ch++) {
                        extraBits[ch][i] = bits.readBits(extra);
                    }
                }
            }
            for (int ch = 0; ch < channels; ch++) {
                int[] error = predictionError[ch];
                riceDecompress(error, frames, bps, riceMult[ch] * config.historyMult() / 4);
                if (predictionType[ch] == 15) {
                    // Two passes: first the fixed first-order predictor, then the coded one.
                    lpcPrediction(error, error, frames, bps, null, 31, 0);
                }
                lpcPrediction(error, out[firstChannel + ch], frames, bps, lpcCoefs[ch], lpcOrder[ch], lpcQuant[ch]);
            }
        } else {
            // Escape: uncompressed samples.
            for (int i = 0; i < frames; i++) {
                for (int ch = 0; ch < channels; ch++) {
                    out[firstChannel + ch][i] = bits.readSigned(config.bitDepth());
                }
            }
            extra = 0;
        }

        if (channels == 2 && decorrLeftWeight != 0) {
            decorrelateStereo(out[firstChannel], out[firstChannel + 1], frames, decorrShift, decorrLeftWeight);
        }
        if (extra > 0) {
            for (int ch = 0; ch < channels; ch++) {
                int[] samples = out[firstChannel + ch];
                int[] low     = extraBits[ch];
                for (int i = 0; i < frames; i++) {
                    samples[i] = (samples[i] << extra) | low[i];
                }
            }
        }
        return frames;
    }

    private void riceDecompress(int[] output, int frames, int bps, int historyMult) {
        int history      = config.initialHistory();
        int signModifier = 0;
        for (int i = 0; i < frames; i++) {
            int k = Math.min(log2((history >>> 9) + 3), config.riceLimit());
            int x = decodeScalar(k, bps) + signModifier;
            signModifier = 0;
            output[i] = (x >>> 1) ^ -(x & 1);

            if (Integer.compareUnsigned(x, 0xFFFF) > 0) {
                history = 0xFFFF;
            } else {
                history += x * historyMult - ((history * historyMult) >>> 9);
            }

            // A run of zeros may follow.
            if (Integer.compareUnsigned(history, 128) < 0 && i + 1 < frames) {
                int blockK    = Math.min(7 - log2(history) + ((history + 16) >>> 6), config.riceLimit());
                int blockSize = decodeScalar(blockK, 16);
                if (blockSize > 0) {
                    if (blockSize >= frames - i) {
                        blockSize = frames - i - 1;            // invalid; keep inside the packet
                    }
                    for (int z = 1; z <= blockSize; z++) {
                        output[i + z] = 0;
                    }
                    i += blockSize;
                }
                if (Integer.compareUnsigned(blockSize, 0xFFFF) <= 0) {
                    signModifier = 1;
                }
                history = 0;
            }
        }
    }

    private int decodeScalar(int k, int bps) {
        int x = bits.readUnaryMax9();
        if (x > 8) {
            return bits.readBits(bps);                  // escape: the value itself
        }
        if (k != 1) {
            int extra = bits.peekBits(k);
            x = (x << k) - x;
            if (extra > 1) {
                x += extra - 1;
                bits.skip(k);
            } else {
                bits.skip(k - 1);
            }
        }
        return x;
    }

    private static void lpcPrediction(int[] error, int[] out, int frames, int bps, int[] coefs, int order, int quant) {
        out[0] = error[0];
        if (frames <= 1) {
            return;
        }
        if (order == 0) {
            System.arraycopy(error, 1, out, 1, frames - 1);
            return;
        }
        if (order == 31) {
            for (int i = 1; i < frames; i++) {
                out[i] = signExtend(out[i - 1] + error[i], bps);
            }
            return;
        }
        int i = 1;
        for (; i <= order && i < frames; i++) {
            out[i] = signExtend(out[i - 1] + error[i], bps);
        }
        for (; i < frames; i++) {
            int base     = i - order;                   // out[base .. i-1] are the predictor's inputs
            int d        = out[base - 1];
            int val      = 0;
            int errorVal = error[i];
            for (int j = 0; j < order; j++) {
                val += (out[base + j] - d) * coefs[j];
            }
            val = (int) (((long) val + (1L << (quant - 1))) >> quant);
            val += d + errorVal;
            out[i] = signExtend(val, bps);

            int errorSign = Integer.signum(errorVal);
            if (errorSign != 0) {
                for (int j = 0; j < order && errorVal * errorSign > 0; j++) {
                    int diff = d - out[base + j];
                    int sign = Integer.signum(diff) * errorSign;
                    coefs[j] -= sign;
                    diff *= sign;
                    errorVal -= (diff >> quant) * (j + 1);
                }
            }
        }
    }

    private static void decorrelateStereo(int[] left, int[] right, int frames, int shift, int weight) {
        for (int i = 0; i < frames; i++) {
            int a = left[i];
            int b = right[i];
            a -= (b * weight) >> shift;
            b += a;
            left[i] = b;
            right[i] = a;
        }
    }

    private static int signExtend(int value, int bits) {
        int shift = 32 - bits;
        return (value << shift) >> shift;
    }

    private static int log2(int value) {
        return value == 0 ? 0 : 31 - Integer.numberOfLeadingZeros(value);
    }
}
