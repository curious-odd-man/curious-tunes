package com.github.curiousoddman.curious_tunes.util;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Set;

/**
 * Album covers as bytes: from an image file or from an image on the clipboard.
 */
public final class ImageBytes {
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "bmp");

    private ImageBytes() {
    }

    public static boolean isImageFile(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        int    dot  = name.lastIndexOf('.');
        return dot >= 0 && IMAGE_EXTENSIONS.contains(name.substring(dot + 1));
    }

    public static byte[] read(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    /**
     * @return the bytes as an image, or null when the bytes are not an image
     */
    public static Image toImage(byte[] bytes) {
        Image image = new Image(new ByteArrayInputStream(bytes));
        return image.isError() ? null : image;
    }

    /**
     * PNG bytes of an image taken from the clipboard.
     */
    public static byte[] toPng(Image image) throws IOException {
        PixelReader reader = image.getPixelReader();
        if (reader == null) {
            throw new IOException("The image cannot be read");
        }
        int           width  = (int) image.getWidth();
        int           height = (int) image.getHeight();
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                target.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(target, "png", out);
        return out.toByteArray();
    }
}
