package com.github.curiousoddman.curious_tunes.ui.playback;

import java.util.function.DoubleUnaryOperator;

/**
 * Finds the largest font size at which the lyrics still fit into the available height.
 * Measuring is delegated, so there is no JavaFX dependency here.
 */
public final class LyricsFitter {
    private static final int MAX_ITERATIONS = 16;
    private static final double PRECISION = 0.25;

    private LyricsFitter() {
    }

    /**
     * @param heightAtSize text height for the given font size
     * @return the biggest size in [minSize, maxSize] whose height does not exceed maxHeight;
     * minSize if even the smallest size does not fit (the text will scroll)
     */
    public static double fit(double minSize,
                             double maxSize,
                             double maxHeight,
                             DoubleUnaryOperator heightAtSize) {
        if (heightAtSize.applyAsDouble(maxSize) <= maxHeight) {
            return maxSize;
        }
        if (heightAtSize.applyAsDouble(minSize) > maxHeight) {
            return minSize;
        }

        double low = minSize;      // fits
        double high = maxSize;     // does not fit
        for (int i = 0; i < MAX_ITERATIONS && high - low > PRECISION; i++) {
            double mid = (low + high) / 2;
            if (heightAtSize.applyAsDouble(mid) <= maxHeight) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return Math.max(minSize, Math.floor(low * 2) / 2);
    }
}
