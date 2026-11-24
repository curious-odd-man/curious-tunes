package com.github.curiousoddman.curious_tunes.domain.covers;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Component;

import static com.github.curiousoddman.curious_tunes.dbobj.Tables.ALBUM;

/**
 * Database access for cover thumbnails and the "set by hand" flag of album covers (migration V007).
 * <p>
 * Uses jOOQ without generated classes for the new table and column, so the checked-in {@code dbobj} code doesn't
 * have to be regenerated; after the next {@code gradle jooqCodegen} the generated {@code ALBUM_COVER_THUMB} table and
 * {@code ALBUM.IMAGE_OVERRIDDEN} field can replace the definitions below.
 */
@Component
@RequiredArgsConstructor
public class AlbumCoverStore {
    private static final Table<Record>  THUMB            = DSL.table(DSL.name("ALBUM_COVER_THUMB"));
    private static final Field<Integer> THUMB_ALBUM_ID   = DSL.field(DSL.name("ALBUM_COVER_THUMB", "ALBUM_ID"), SQLDataType.INTEGER);
    private static final Field<Short>   THUMB_PX         = DSL.field(DSL.name("ALBUM_COVER_THUMB", "PX"), SQLDataType.SMALLINT);
    private static final Field<String>  THUMB_SOURCE_MD5 = DSL.field(DSL.name("ALBUM_COVER_THUMB", "SOURCE_MD5"), SQLDataType.VARCHAR(32));
    private static final Field<byte[]>  THUMB_JPEG       = DSL.field(DSL.name("ALBUM_COVER_THUMB", "JPEG"), SQLDataType.VARBINARY);
    private static final Field<Boolean> IMAGE_OVERRIDDEN = DSL.field(DSL.name("ALBUM", "IMAGE_OVERRIDDEN"), SQLDataType.BOOLEAN);

    private final DSLContext dsl;

    /**
     * @return the thumbnail of {@code px} made from the cover with this MD5, or null if there is none (or it was
     * made from another cover)
     */
    public byte[] thumbnail(int albumId, int px, String sourceMd5) {
        return dsl.select(THUMB_JPEG)
                  .from(THUMB)
                  .where(THUMB_ALBUM_ID.eq(albumId), THUMB_PX.eq((short) px), THUMB_SOURCE_MD5.eq(sourceMd5))
                  .fetchOne(THUMB_JPEG);
    }

    /**
     * Stores the thumbnail, replacing any older one of the same album and size.
     */
    public void saveThumbnail(int albumId, int px, String sourceMd5, byte[] jpeg) {
        dsl.mergeInto(THUMB)
           .using(dsl.selectOne())
           .on(THUMB_ALBUM_ID.eq(albumId), THUMB_PX.eq((short) px))
           .whenMatchedThenUpdate()
           .set(THUMB_SOURCE_MD5, sourceMd5)
           .set(THUMB_JPEG, jpeg)
           .whenNotMatchedThenInsert(THUMB_ALBUM_ID, THUMB_PX, THUMB_SOURCE_MD5, THUMB_JPEG)
           .values(DSL.val(albumId), DSL.val((short) px), DSL.val(sourceMd5), DSL.val(jpeg))
           .execute();
    }

    /**
     * @return true when the album's cover was set by hand in the tag editor, so a rescan keeps it
     */
    public boolean isOverridden(int albumId) {
        Boolean overridden = dsl.select(IMAGE_OVERRIDDEN)
                                .from(ALBUM)
                                .where(ALBUM.ID.eq(albumId))
                                .fetchOne(IMAGE_OVERRIDDEN);
        return Boolean.TRUE.equals(overridden);
    }

    public void setOverridden(int albumId, boolean overridden) {
        dsl.update(ALBUM)
           .set(IMAGE_OVERRIDDEN, overridden)
           .where(ALBUM.ID.eq(albumId))
           .execute();
    }

    /**
     * Replaces the album's cover with one read from its files (a rescan, item 6.5). Its thumbnails stop matching and
     * are made again when next shown.
     */
    public void replaceCoverFromFiles(int albumId, byte[] cover) {
        dsl.update(ALBUM)
           .set(ALBUM.IMAGE, cover)
           .where(ALBUM.ID.eq(albumId))
           .execute();
    }
}
