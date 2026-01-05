package com.github.curiousoddman.curious_tunes.util;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;

@Slf4j
@UtilityClass
public class ImageUtils {
    public static void setImageIfPresent(AlbumRecord albumRecord, ImageView imageView) {
        if (albumRecord.getImage() != null) {
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(albumRecord.getImage())) {
                Image value = new Image(byteArrayInputStream);
                imageView.setImage(value);
            } catch (Exception e) {
                log.error("Unable to display image", e);
            }
        }
    }
}
