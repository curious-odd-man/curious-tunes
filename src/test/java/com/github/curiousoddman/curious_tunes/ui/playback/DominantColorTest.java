package com.github.curiousoddman.curious_tunes.ui.playback;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DominantColorTest {

    @Test
    void picksTheDominantVividColor() {
        int[] pixels = new int[100];
        Arrays.fill(pixels, 0, 70, 0xFFC81E1E);       // red
        Arrays.fill(pixels, 70, 90, 0xFF808080);      // gray
        Arrays.fill(pixels, 90, 100, 0xFF000000);     // black
        int color = DominantColor.extract(pixels);
        assertEquals(0xC81E1E, color);
    }

    @Test
    void vividColorBeatsLargerGrayArea() {
        int[] pixels = new int[100];
        Arrays.fill(pixels, 0, 40, 0xFF2040E0);
        Arrays.fill(pixels, 40, 100, 0xFF707070);
        assertEquals(0x2040E0, DominantColor.extract(pixels));
    }

    @Test
    void grayImageGivesGray() {
        int[] pixels = new int[20];
        Arrays.fill(pixels, 0xFF808080);
        assertEquals(0x808080, DominantColor.extract(pixels));
    }

    @Test
    void blackAndWhiteImageFallsBackToAverage() {
        int[] pixels = {0xFF000000, 0xFFFFFFFF, 0xFF000000, 0xFFFFFFFF};
        int color = DominantColor.extract(pixels);
        int red = (color >> 16) & 0xFF;
        assertTrue(red >= 120 && red <= 135);
    }

    @Test
    void noOpaquePixelsGivesMinusOne() {
        assertEquals(-1, DominantColor.extract(new int[0]));
        assertEquals(-1, DominantColor.extract(new int[5]));
    }
}
