package com.github.curiousoddman.curious_tunes.domain.covers;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;

/**
 * Makes the small JPEG copies of album covers shown in lists and grids (doc/performance-review.md, item 2.1).
 * Pure Java ({@code java.desktop}): JavaFX can decode images but not encode them.
 */
public final class CoverThumbnails {
    /**
     * The thumbnail sizes kept: lists and small cards, large cards and the tag editor.
     */
    public static final int   SMALL        = 128;
    public static final int   LARGE        = 512;
    static final        float JPEG_QUALITY = 0.85f;

    private CoverThumbnails() {
    }

    /**
     * @return the size of the thumbnail to use for a picture {@code pixels} wide, or 0 when the original should
     * be used (larger than {@link #LARGE})
     */
    public static int sizeFor(int pixels) {
        if (pixels <= SMALL) {
            return SMALL;
        }
        return pixels <= LARGE ? LARGE : 0;
    }

    /**
     * Scales the cover so its longer side is {@code size} pixels (smaller covers keep their size) and encodes it
     * as JPEG. Transparent parts become white.
     *
     * @return the JPEG bytes, or null if the bytes are not an image Java can read
     */
    public static byte[] make(byte[] cover, int size) throws IOException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(cover));
        if (source == null) {
            return null;
        }
        double scale  = Math.min(1.0, (double) size / Math.max(source.getWidth(), source.getHeight()));
        int    width  = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int    height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        return encodeJpeg(scaleDown(source, width, height));
    }

    /**
     * @return the MD5 of the bytes, as 32 lower-case hex digits
     */
    public static String md5(byte[] bytes) {
        try {
            return HexFormat.of()
                            .formatHex(MessageDigest.getInstance("MD5")
                                                    .digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Halves the size in steps before the last, bilinear step: one big bilinear jump from, say, 3000 px to 128 px
     * skips most pixels and looks grainy.
     */
    private static BufferedImage scaleDown(BufferedImage source, int width, int height) {
        BufferedImage current = toRgb(source);
        int           w       = current.getWidth();
        int           h       = current.getHeight();
        while (w / 2 >= width && h / 2 >= height) {
            w /= 2;
            h /= 2;
            current = draw(current, w, h);
        }
        return w == width && h == height ? current : draw(current, width, height);
    }

    private static BufferedImage toRgb(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_RGB) {
            return image;
        }
        return draw(image, image.getWidth(), image.getHeight());
    }

    private static BufferedImage draw(BufferedImage source, int width, int height) {
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D    g      = target.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    private static byte[] encodeJpeg(BufferedImage image) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer");
        }
        ImageWriter           writer = writers.next();
        ByteArrayOutputStream out    = new ByteArrayOutputStream();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(stream);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }
}
