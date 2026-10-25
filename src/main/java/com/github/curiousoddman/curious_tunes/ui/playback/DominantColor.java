package com.github.curiousoddman.curious_tunes.ui.playback;

/**
 * Picks a representative "vivid" color from an image. No JavaFX dependency, so it can be unit tested.
 */
public final class DominantColor {
    private static final int LEVELS = 16;        // buckets per channel
    private static final int SHIFT = 4;          // 256 / 16 = 2^4

    private DominantColor() {
    }

    /**
     * @param argbPixels pixels in 0xAARRGGBB format
     * @return dominant color as 0xRRGGBB, or -1 if there are no opaque pixels
     */
    public static int extract(int[] argbPixels) {
        double[] weight = new double[LEVELS * LEVELS * LEVELS];
        double[] sumR = new double[weight.length];
        double[] sumG = new double[weight.length];
        double[] sumB = new double[weight.length];

        long opaque = 0;
        long totalR = 0;
        long totalG = 0;
        long totalB = 0;

        for (int argb : argbPixels) {
            if (((argb >>> 24) & 0xFF) < 128) {
                continue;
            }
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;

            opaque++;
            totalR += r;
            totalG += g;
            totalB += b;

            int max = Math.max(r, Math.max(g, b));
            int min = Math.min(r, Math.min(g, b));
            double value = max / 255.0;
            double saturation = max == 0 ? 0 : (max - min) / (double) max;

            boolean nearBlack = value < 0.12;
            boolean nearWhite = saturation < 0.10 && value > 0.92;
            if (nearBlack || nearWhite) {
                continue;
            }

            int bucket = ((r >> SHIFT) * LEVELS + (g >> SHIFT)) * LEVELS + (b >> SHIFT);
            double w = 0.1 + saturation;
            weight[bucket] += w;
            sumR[bucket] += r * w;
            sumG[bucket] += g * w;
            sumB[bucket] += b * w;
        }

        if (opaque == 0) {
            return -1;
        }

        int best = -1;
        double bestWeight = 0;
        for (int i = 0; i < weight.length; i++) {
            if (weight[i] > bestWeight) {
                bestWeight = weight[i];
                best = i;
            }
        }

        if (best < 0) {
            // Only near-black / near-white pixels: use the plain average.
            return pack(totalR / opaque, totalG / opaque, totalB / opaque);
        }
        return pack(
                Math.round(sumR[best] / weight[best]),
                Math.round(sumG[best] / weight[best]),
                Math.round(sumB[best] / weight[best])
        );
    }

    private static int pack(long r, long g, long b) {
        return (int) ((r << 16) | (g << 8) | b);
    }
}
