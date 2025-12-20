package com.github.curiousoddman.curious_tunes.backend;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import static com.github.curiousoddman.curious_tunes.dbobj.Tables.TRACK;
import static com.github.curiousoddman.curious_tunes.dbobj.tables.Album.ALBUM;
import static com.github.curiousoddman.curious_tunes.dbobj.tables.Artist.ARTIST;


@Component
@RequiredArgsConstructor
public class DataAccess {
    public static class Caches {
        private static final String ARTISTS = "artists";
        private static final String ALBUMS = "albums";
    }

    private final DefaultDSLContext dsl;

    @Cacheable(Caches.ARTISTS)
    public ArtistRecord getOrInsertArtist(String artist) {
        ArtistRecord artistRecord = dsl.selectFrom(ARTIST)
                .where(ARTIST.NAME.eq(artist))
                .fetchOne();
        if (artistRecord != null) {
            return artistRecord;
        }

        return dsl
                .insertInto(ARTIST)
                .values(ARTIST.NAME.eq(artist))
                .returning()
                .fetchOne();
    }

    @Cacheable(Caches.ALBUMS)
    public AlbumRecord getOrInsertAlbum(Integer artistId, String album) {
        AlbumRecord albumRecord = dsl.selectFrom(ALBUM)
                .where(
                        ALBUM.FK_ARTIST.eq(artistId),
                        ALBUM.NAME.eq(album)
                )
                .fetchOne();
        if (albumRecord != null) {
            return albumRecord;
        }

        return dsl
                .insertInto(ALBUM)
                .values(ALBUM.FK_ARTIST.eq(artistId), ALBUM.NAME.eq(album))
                .returning()
                .fetchOne();
    }

    public TrackRecord getTrack(Integer albumFk, String title) {
        return dsl
                .selectFrom(TRACK)
                .where(
                        TRACK.FK_ALBUM.eq(albumFk),
                        TRACK.TITLE.eq(title)
                ).fetchOne();
    }
}
