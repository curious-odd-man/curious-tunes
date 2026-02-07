package com.github.curiousoddman.curious_tunes.backend.tags;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.util.JooqUtils;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.github.curiousoddman.curious_tunes.dbobj.tables.Track.TRACK;

@Slf4j
@Getter
@RequiredArgsConstructor
public abstract class MetadataTagsBase implements MetadataTags {
    protected final String fileLocation;
    protected String artist;
    protected String album;
    protected String title;
    protected Integer trackNumber;
    protected String releaseDate;
    protected Integer diskNumber;
    protected Integer sampleRate;
    protected String genre;
    protected String composer;
    protected Long duration;
    protected String lyrics;
    protected AlbumCover albumCover;

    @Override
    public <T> T validate(String description, T field, T newValue) {
        if (field == null) {
            return newValue;
        }
        if (!Objects.equals(field, newValue)) {
            log.info("Different value duplicate box detected {} --> {}:{}", description, field, newValue);
        }
        return newValue;
    }

    @Override
    public boolean updateTrackIfChanged(TrackRecord trackRecord) {
        JooqUtils.updateFieldIfChanged(trackRecord, fileLocation, TRACK.FILE_LOCATION);
        // Those 3 cannot really change - they belong to album and artist
//        updateFieldIfChanged(trackRecord, artist, TRACK.);
//        updateFieldIfChanged(trackRecord, album, TRACK.);
        // updateFieldIfChanged(trackRecord, albumCover, TRACK.);
        JooqUtils.updateFieldIfChanged(trackRecord, title, TRACK.TITLE);
        JooqUtils.updateFieldIfChanged(trackRecord, trackNumber, TRACK.TRACK_NUMBER);
        JooqUtils.updateFieldIfChanged(trackRecord, releaseDate, TRACK.RELEASE_DATE);
        JooqUtils.updateFieldIfChanged(trackRecord, diskNumber, TRACK.DISK_NUMBER);
        JooqUtils.updateFieldIfChanged(trackRecord, sampleRate, TRACK.AUDIO_SAMPLE_RATE);
        JooqUtils.updateFieldIfChanged(trackRecord, genre, TRACK.GENRE);
        JooqUtils.updateFieldIfChanged(trackRecord, composer, TRACK.COMPOSER);
        JooqUtils.updateFieldIfChanged(trackRecord, duration, TRACK.DURATION);
        JooqUtils.updateFieldIfChanged(trackRecord, lyrics, TRACK.LYRICS);
        return trackRecord.changed();
    }

    @Override
    public void setArtist(String artist) {
        this.artist = artist;
        onArtistUpdated();
    }

    protected abstract void onArtistUpdated();

    @Override
    public void setAlbum(String album) {
        this.album = album;
        onAlbumUpdated();
    }

    protected abstract void onAlbumUpdated();

    @Override
    public void setTitle(String title) {
        this.title = title;
        onTitleUpdated();
    }

    protected abstract void onTitleUpdated();

    @Override
    public void setTrackNumber(Integer trackNumber) {
        this.trackNumber = trackNumber;
        onTrackNumberUpdated();
    }

    protected abstract void onTrackNumberUpdated();

    @Override
    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
        onReleaseDateUpdated();
    }

    protected abstract void onReleaseDateUpdated();

    @Override
    public void setDiskNumber(Integer diskNumber) {
        this.diskNumber = diskNumber;
        onDiskNumberUpdated();
    }

    protected abstract void onDiskNumberUpdated();

    @Override
    public void setGenre(String genre) {
        this.genre = genre;
        onGenreUpdated();
    }

    protected abstract void onGenreUpdated();

    @Override
    public void setComposer(String composer) {
        this.composer = composer;
        onComposerUpdated();
    }

    protected abstract void onComposerUpdated();

    @Override
    public void setLyrics(String lyrics) {
        this.lyrics = lyrics;
        onLyricsUpdated();
    }

    protected abstract void onLyricsUpdated();

    @Override
    public void setAlbumCover(AlbumCover albumCover) {
        this.albumCover = albumCover;
        onAlbumCoverUpdated();
    }

    protected abstract void onAlbumCoverUpdated();

    protected void verifyRequiredValuesPresent() {
        List<String> missingFields = new ArrayList<>();

        verifyField(artist, () -> missingFields.add("artist"));
        verifyField(album, () -> missingFields.add("album"));
        verifyField(title, () -> missingFields.add("title"));
        verifyField(trackNumber, () -> missingFields.add("trackNumber"));
        verifyField(duration, () -> missingFields.add("duration"));
        if (!missingFields.isEmpty()) {
            log.error("Following fields are not set: {}", missingFields);
        }
    }

    private static void verifyField(Object o, Runnable onNull) {
        if (o == null) {
            onNull.run();
        }
    }
}
