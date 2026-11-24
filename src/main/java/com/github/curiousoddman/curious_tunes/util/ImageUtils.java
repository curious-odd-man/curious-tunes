package com.github.curiousoddman.curious_tunes.util;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.model.info.AlbumInfo;
import javafx.scene.image.ImageView;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@UtilityClass
public class ImageUtils {
    public static void setImageIfPresent(AlbumInfo albumInfo, ImageView imageView) {
        setImageIfPresent(albumInfo.getImage(), imageView);
    }

    public static void setImageIfPresent(AlbumRecord albumRecord, ImageView imageView) {
        setImageIfPresent(albumRecord.getImage(), imageView);
    }

    /**
     * Shows the cover decoded for the view's size, from the shared cover cache, off the JavaFX thread
     * ({@link CoverImages}). Does nothing without a cover, so the view keeps its placeholder.
     */
    public static void setImageIfPresent(byte[] image, ImageView imageView) {
        CoverImages.show(imageView, image, Math.max(imageView.getFitWidth(), imageView.getFitHeight()));
    }
}
