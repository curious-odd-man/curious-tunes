package com.github.curiousoddman.curious_tunes.domain.covers;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoverThumbnailsTest {

    @Test
    void scalesTheLongerSideToTheSizeAndKeepsTheShape() throws Exception {
        byte[]        cover     = picture(1400, 1000, "png", BufferedImage.TYPE_INT_ARGB);
        byte[]        thumbnail = CoverThumbnails.make(cover, CoverThumbnails.SMALL);
        BufferedImage read      = ImageIO.read(new ByteArrayInputStream(thumbnail));
        assertEquals(128, read.getWidth());
        assertEquals(91, read.getHeight());          // 1000 × 128 / 1400, rounded
        assertTrue(thumbnail[0] == (byte) 0xFF && thumbnail[1] == (byte) 0xD8, "a JPEG");
        assertTrue(thumbnail.length < 20_000, "small: " + thumbnail.length + " bytes");
    }

    @Test
    void keepsTheColoursAndMakesTransparencyWhite() throws Exception {
        byte[]        cover = picture(1000, 1000, "png", BufferedImage.TYPE_INT_ARGB);
        BufferedImage read  = ImageIO.read(new ByteArrayInputStream(CoverThumbnails.make(cover, 512)));
        Color         left  = new Color(read.getRGB(100, 256));      // red half
        Color         right = new Color(read.getRGB(400, 256));      // transparent half
        assertTrue(left.getRed() > 200 && left.getGreen() < 60 && left.getBlue() < 60, left.toString());
        assertTrue(right.getRed() > 240 && right.getGreen() > 240 && right.getBlue() > 240, right.toString());
    }

    @Test
    void neverEnlargesASmallCover() throws Exception {
        byte[]        cover = picture(100, 80, "jpg", BufferedImage.TYPE_INT_RGB);
        BufferedImage read  = ImageIO.read(new ByteArrayInputStream(CoverThumbnails.make(cover, 512)));
        assertEquals(100, read.getWidth());
        assertEquals(80, read.getHeight());
    }

    @Test
    void notAnImageGivesNull() throws Exception {
        assertNull(CoverThumbnails.make(new byte[]{1, 2, 3, 4}, 128));
    }

    @Test
    void picksTheThumbnailSizeForADisplaySize() {
        assertEquals(128, CoverThumbnails.sizeFor(48));
        assertEquals(128, CoverThumbnails.sizeFor(128));
        assertEquals(512, CoverThumbnails.sizeFor(256));
        assertEquals(512, CoverThumbnails.sizeFor(512));
        assertEquals(0, CoverThumbnails.sizeFor(1024));
        assertEquals("900150983cd24fb0d6963f7d28e17f72", CoverThumbnails.md5("abc".getBytes()));
    }

    /**
     * Left half red, right half transparent (or black where there is no alpha).
     */
    private static byte[] picture(int width, int height, String format, int type) throws Exception {
        BufferedImage image = new BufferedImage(width, height, type);
        Graphics2D    g     = image.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, width / 2, height);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }
}
