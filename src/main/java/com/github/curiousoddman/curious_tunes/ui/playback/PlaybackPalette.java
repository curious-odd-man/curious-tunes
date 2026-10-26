package com.github.curiousoddman.curious_tunes.ui.playback;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.util.Locale;

/**
 * Colors of the playback window. The window is always dark with light text, so text stays readable
 * whatever the album art looks like; only the hue/saturation follow the art.
 */
@Slf4j
public record PlaybackPalette(Color bgTop,
                              Color bgBottom,
                              Color text,
                              Color textDim,
                              Color accent,
                              Color unplayed) {

    private static final int SAMPLE_SIZE = 48;

    /**
     * Used when there is no album art. Waveform colors match the library view.
     */
    public static final PlaybackPalette DEFAULT = new PlaybackPalette(
            Color.web("#2d353e"),
            Color.web("#161a1e"),
            Color.web("#f2f2f2"),
            Color.web("#f2f2f2", 0.68),
            Color.web("#ffa500"),
            Color.web("#8fa5b2")
    );

    /**
     * Decodes (a small version of) the image and derives the palette. Safe to call off the FX thread.
     */
    public static PlaybackPalette fromImage(byte[] imageBytes) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(imageBytes)) {
            Image       image  = new Image(in, SAMPLE_SIZE, SAMPLE_SIZE, true, false);
            int         width  = (int) image.getWidth();
            int         height = (int) image.getHeight();
            PixelReader reader = image.getPixelReader();
            if (image.isError() || reader == null || width <= 0 || height <= 0) {
                return DEFAULT;
            }
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    pixels[y * width + x] = reader.getArgb(x, y);
                }
            }
            int rgb = DominantColor.extract(pixels);
            return rgb < 0 ? DEFAULT : fromRgb(rgb);
        } catch (Exception e) {
            log.warn("Unable to derive colors from album art", e);
            return DEFAULT;
        }
    }

    public static PlaybackPalette fromRgb(int rgb) {
        Color  base       = Color.rgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        double hue        = base.getHue();
        double saturation = base.getSaturation();
        double brightness = base.getBrightness();

        if (saturation < 0.12) {
            // Gray-ish art: neutral background, default waveform colors.
            double top = clamp(brightness * 0.40, 0.16, 0.30);
            return new PlaybackPalette(
                    Color.hsb(0, 0, top),
                    Color.hsb(0, 0, top * 0.55),
                    DEFAULT.text, DEFAULT.textDim, DEFAULT.accent, DEFAULT.unplayed
            );
        }

        double sat = Math.min(saturation, 0.85);
        return new PlaybackPalette(
                Color.hsb(hue, sat * 0.80, clamp(brightness * 0.45, 0.20, 0.34)),
                Color.hsb(hue, sat * 0.85, clamp(brightness * 0.25, 0.10, 0.18)),
                DEFAULT.text,
                DEFAULT.textDim,
                Color.hsb(hue, clamp(saturation, 0.50, 0.95), 0.97),
                Color.hsb(hue, sat * 0.25, 0.62)
        );
    }

    public PlaybackPalette interpolate(PlaybackPalette other, double t) {
        return new PlaybackPalette(
                bgTop.interpolate(other.bgTop, t),
                bgBottom.interpolate(other.bgBottom, t),
                text.interpolate(other.text, t),
                textDim.interpolate(other.textDim, t),
                accent.interpolate(other.accent, t),
                unplayed.interpolate(other.unplayed, t)
        );
    }

    /**
     * Looked-up color definitions for the root node; the stylesheet refers to them.
     */
    public String toCssVariables() {
        return "-pw-bg-top: %s; -pw-bg-bottom: %s; -pw-text: %s; -pw-text-dim: %s; -pw-accent: %s;"
                .formatted(css(bgTop), css(bgBottom), css(text), css(textDim), css(accent));
    }

    private static String css(Color c) {
        return String.format(Locale.ROOT, "rgba(%d,%d,%d,%.3f)",
                Math.round(c.getRed() * 255),
                Math.round(c.getGreen() * 255),
                Math.round(c.getBlue() * 255),
                c.getOpacity());
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
