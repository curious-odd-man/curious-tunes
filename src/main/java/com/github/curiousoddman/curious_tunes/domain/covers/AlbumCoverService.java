package com.github.curiousoddman.curious_tunes.domain.covers;

import com.github.curiousoddman.curious_tunes.util.CoverImages;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Hands {@link CoverImages} a small thumbnail instead of the full-size cover when a cover is shown small
 * (doc/performance-review.md, item 2.1): from the database when one was made from this very cover, otherwise made
 * now and stored. Called on the cover decoder threads, never on the JavaFX thread.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlbumCoverService implements CoverImages.ThumbnailSource {
    private final AlbumCoverStore store;

    @PostConstruct
    void register() {
        CoverImages.setThumbnailSource(this);
    }

    @Override
    public byte[] thumbnail(int albumId, byte[] cover, int px) {
        String md5 = CoverThumbnails.md5(cover);
        try {
            byte[] stored = store.thumbnail(albumId, px, md5);
            if (stored != null) {
                return stored;
            }
        } catch (RuntimeException e) {
            log.warn("Cannot read the thumbnail of album {}", albumId, e);
        }
        try {
            byte[] made = CoverThumbnails.make(cover, px);
            if (made == null) {
                return cover;                           // not a format Java can read: let JavaFX decode the original
            }
            try {
                store.saveThumbnail(albumId, px, md5, made);
            } catch (RuntimeException e) {
                log.warn("Cannot store the thumbnail of album {}", albumId, e);
            }
            return made;
        } catch (Exception e) {
            log.warn("Cannot make a thumbnail of album {}", albumId, e);
            return cover;
        }
    }
}
