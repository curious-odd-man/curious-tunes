package com.github.curiousoddman.curious_tunes.domain.audio.alac;

/**
 * Reads bits most significant first from a byte array, as ALAC packets are written. Reading past the end gives
 * zero bits; {@link #bitsLeft()} tells when the data is used up.
 */
final class BitReader {
    private byte[] data;
    private int    lengthBits;
    private int    position;            // in bits

    void reset(byte[] data, int length) {
        this.data = data;
        this.lengthBits = length * 8;
        this.position = 0;
    }

    int bitsLeft() {
        return lengthBits - position;
    }

    int readBit() {
        int index = position >>> 3;
        int bit   = index < data.length && position < lengthBits ? (data[index] >>> (7 - (position & 7))) & 1 : 0;
        position++;
        return bit;
    }

    /**
     * @param count 0–32 bits
     * @return the bits as an unsigned value (for 32 bits, the raw 32-bit pattern)
     */
    int readBits(int count) {
        return (int) readBitsLong(count);
    }

    /**
     * @param count 0–32 bits, read as a signed (two's complement) number
     */
    int readSigned(int count) {
        if (count == 0) {
            return 0;
        }
        int value = readBits(count);
        int shift = 32 - count;
        return (value << shift) >> shift;
    }

    int peekBits(int count) {
        int saved = position;
        int value = readBits(count);
        position = saved;
        return value;
    }

    void skip(int count) {
        position += count;
    }

    /**
     * Counts 1 bits until a 0 bit, at most 9 (the 0 bit is consumed when found within the 9).
     */
    int readUnaryMax9() {
        for (int count = 0; count < 9; count++) {
            if (readBit() == 0) {
                return count;
            }
        }
        return 9;
    }

    private long readBitsLong(int count) {
        long value = 0;
        int  read  = 0;
        while (read < count) {
            int byteIndex = position >>> 3;
            int bitOffset = position & 7;
            int available = 8 - bitOffset;
            int take      = Math.min(available, count - read);
            int b         = byteIndex < data.length && position < lengthBits ? data[byteIndex] & 0xFF : 0;
            int bits      = (b >>> (available - take)) & ((1 << take) - 1);
            value = (value << take) | bits;
            position += take;
            read += take;
        }
        return value;
    }
}
