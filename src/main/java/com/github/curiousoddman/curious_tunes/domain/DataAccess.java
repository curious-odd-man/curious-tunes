package com.github.curiousoddman.curious_tunes.domain;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.*;
import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
import com.github.curiousoddman.curious_tunes.model.playlist.PlaylistItem;
import lombok.RequiredArgsConstructor;
import org.jooq.TableField;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.github.curiousoddman.curious_tunes.dbobj.Tables.*;
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

    private Map<Integer, List<TrackOverridesHistoryRecord>> trackOverrides;

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
                .columns(ARTIST.NAME)
                .values(artist)
                .returning()
                .fetchOne();
    }

    @Cacheable(Caches.ALBUMS)
    public AlbumRecord getOrInsertAlbum(Integer artistId, String album, byte[] image) {
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
                .columns(ALBUM.FK_ARTIST, ALBUM.NAME, ALBUM.IMAGE)
                .values(artistId, album, image)
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

    public void insertTrack(TrackRecord newTrackRecord) {
        dsl
                .insertInto(TRACK)
                .set(newTrackRecord)
                .execute();
    }

    public List<ArtistRecord> getAllArtists() {
        return dsl
                .selectFrom(ARTIST)
                .stream()
                .toList();
    }

    public List<AlbumRecord> getArtistAlbums(int artistFk) {
        return dsl
                .selectFrom(ALBUM)
                .where(ALBUM.FK_ARTIST.eq(artistFk))
                .stream()
                .toList();
    }

    public List<TrackInfo> getAlbumsTracks(List<AlbumRecord> albums) {
        Map<Integer, AlbumRecord> albumFks = albums
                .stream()
                .collect(Collectors.toMap(AlbumRecord::getId, Function.identity()));

        return dsl
                .selectFrom(TRACK)
                .where(TRACK.FK_ALBUM.in(albumFks.keySet()))
                .stream()
                .map(trackRecord -> new TrackInfo(trackRecord, null, albumFks.get(trackRecord.getFkAlbum())))
                .toList();
    }

    public List<TrackRecord> getAlbumTracks(int albumFk) {
        return dsl
                .selectFrom(TRACK)
                .where(TRACK.FK_ALBUM.eq(albumFk))
                .stream()
                .toList();
    }

    public List<TrackOverridesHistoryRecord> getTrackOverrides(int id) {
        return initTrackOverrides().getOrDefault(id, List.of());
    }

    private Map<Integer, List<TrackOverridesHistoryRecord>> initTrackOverrides() {
        if (trackOverrides == null) {
            trackOverrides = dsl
                    .selectFrom(TRACK_OVERRIDES_HISTORY)
                    .fetchStream()
                    .collect(Collectors.groupingBy(
                            TrackOverridesHistoryRecord::getTrackId
                    ));
        }
        return trackOverrides;
    }

    public List<TrackInfo> getArtistTracks(ArtistRecord artistRecord) {
        List<AlbumRecord> artistAlbums = getArtistAlbums(artistRecord.getId());
        return getAlbumsTracks(artistAlbums);
    }

    public Map<TrackInfo, Map.Entry<AlbumRecord, ArtistRecord>> getArtistAlbumForTracks(List<PlaylistItem> playlistItems) {
        if (playlistItems.isEmpty()) {
            return Map.of();
        }
        Set<Integer> albumFks = playlistItems
                .stream()
                .map(PlaylistItem::getTrackRecord)
                .map(TrackRecord::getFkAlbum)
                .collect(Collectors.toSet());
        Map<Integer, AlbumRecord> albumIdToRecord = dsl
                .selectFrom(ALBUM)
                .where(ALBUM.ID.in(albumFks))
                .fetch()
                .intoMap(AlbumRecord::getId);
        Set<Integer> artistFks = albumIdToRecord.values().stream().map(AlbumRecord::getFkArtist).collect(Collectors.toSet());
        Map<Integer, ArtistRecord> artistIdToRecord = dsl
                .selectFrom(ARTIST)
                .where(ARTIST.ID.in(artistFks))
                .fetch()
                .intoMap(ArtistRecord::getId);

        return playlistItems
                .stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        tr -> {
                            AlbumRecord albumRecord = albumIdToRecord.get(tr.getTrackRecord().getFkAlbum());
                            ArtistRecord artistRecord = artistIdToRecord.get(albumRecord.getFkArtist());
                            return Map.entry(
                                    albumRecord,
                                    artistRecord
                            );
                        },
                        (r1, r2) -> {
                            throw new IllegalArgumentException();
                        },
                        LinkedHashMap::new
                ));
    }

    public void insertIntoHistory(OffsetDateTime now, Integer trackId, int volume) {
        dsl
                .insertInto(PLAYBACK_HISTORY)
                .columns(PLAYBACK_HISTORY.DATETIME, PLAYBACK_HISTORY.FK_TRACK, PLAYBACK_HISTORY.VOLUME)
                .values(now, trackId, (byte) volume)
                .execute();
    }

    public List<PlaybackHistoryRecord> getAllHistoryRecords() {
        return dsl
                .selectFrom(PLAYBACK_HISTORY)
                .orderBy(PLAYBACK_HISTORY.DATETIME.desc())
                .stream()
                .toList();
    }

    public List<TrackRecord> getTracks(List<Integer> trackIds) {
        return dsl
                .selectFrom(TRACK)
                .where(TRACK.ID.in(trackIds))
                .stream()
                .toList();
    }

    public AlbumRecord getAlbum(Integer id) {
        return dsl.fetchSingle(ALBUM, ALBUM.ID.eq(id));
    }

    public ArtistRecord getArtist(Integer id) {
        return dsl.fetchSingle(ARTIST, ARTIST.ID.eq(id));
    }

    public List<ArtistRecord> getArtists(Set<Integer> artistFks) {
        return dsl.fetch(ARTIST, ARTIST.ID.in(artistFks));
    }

    public void storeTrackOverride(TrackInfo trackInfo, TableField<TrackRecord, ?> field, String text) {
        TrackOverridesHistoryRecord insertedRow = dsl.insertInto(TRACK_OVERRIDES_HISTORY)
                .set(TRACK_OVERRIDES_HISTORY.TRACK_ID, trackInfo.getTrackId())
                .set(TRACK_OVERRIDES_HISTORY.FIELD, field.getName())
                .set(TRACK_OVERRIDES_HISTORY.OLD_VALUE, text)
                .set(TRACK_OVERRIDES_HISTORY.MODIFIED_AT, LocalDateTime.now())
                .returning()
                .fetchOne();

        initTrackOverrides()
                .computeIfAbsent(insertedRow.getTrackId(), k -> new ArrayList<>())
                .add(insertedRow);
    }

    public Optional<TrackRecord> getTrack(Path file) {
        return dsl
                .selectFrom(TRACK)
                .where(TRACK.FILE_LOCATION.eq(file.toString()))
                .fetchOptional();

    }
}
