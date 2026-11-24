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
        CoverImages.show(imageView, albumInfo.getId(), albumInfo.getImage(),
                Math.max(imageView.getFitWidth(), imageView.getFitHeight()));
    }

    /**
     * Like {@link #setImageIfPresent(byte[], ImageView)}; knowing the album, small covers come from a stored
     * thumbnail (doc/performance-review.md, item 2.1).
     */
    public static void setImageIfPresent(AlbumRecord albumRecord, ImageView imageView) {
        CoverImages.show(imageView, albumRecord.getId(), albumRecord.getImage(),
                Math.max(imageView.getFitWidth(), imageView.getFitHeight()));
    }

    /**
     * Shows the cover decoded for the view's size, from the shared cover cache, off the JavaFX thread
     * ({@link CoverImages}). Does nothing without a cover, so the view keeps its placeholder.
     */
    public static void setImageIfPresent(byte[] image, ImageView imageView) {
        CoverImages.show(imageView, image, Math.max(imageView.getFitWidth(), imageView.getFitHeight()));
    }
}
