package com.github.curiousoddman.curious_tunes.domain.tags;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.github.curiousoddman.curious_tunes.dbobj.tables.Track.TRACK;
import static com.github.curiousoddman.curious_tunes.util.JooqUtils.updateFieldIfChanged;

@Slf4j
@Getter
@RequiredArgsConstructor
public abstract class MetadataTagsBase implements MetadataTags {
    protected final String     fileLocation;
    protected       String     artist;
    protected       String     album;
    protected       String     title;
    protected       Integer    trackNumber;
    protected       String     releaseDate;
    protected       Integer    diskNumber;
    protected       Integer    sampleRate;
    protected       String     genre;
    protected       String     composer;
    protected       Long       duration;
    protected       String     lyrics;
    protected       AlbumCover albumCover;

    /**
     * @return {@code newValue}; logs when a file has the same tag twice with different values
     */
    protected <T> T validate(String description, T field, T newValue) {
        if (field == null) {
            return newValue;
        }
        if (!Objects.equals(field, newValue)) {
            log.info("Different value duplicate box detected {} --> {}:{}", description, field, newValue);
        }
        return newValue;
    }

    @Override
    public boolean updateTrackIfChanged(TrackRecord trackRecord, Integer newAlbumId) {
        // Artist and cover aren't track fields: they belong to the album and the artist.
        updateFieldIfChanged(trackRecord, newAlbumId, TRACK.FK_ALBUM);
        updateFieldIfChanged(trackRecord, fileLocation, TRACK.FILE_LOCATION);
        updateFieldIfChanged(trackRecord, title, TRACK.TITLE);
        updateFieldIfChanged(trackRecord, trackNumber, TRACK.TRACK_NUMBER);
        updateFieldIfChanged(trackRecord, releaseDate, TRACK.RELEASE_DATE);
        updateFieldIfChanged(trackRecord, diskNumber, TRACK.DISK_NUMBER);
        updateFieldIfChanged(trackRecord, sampleRate, TRACK.AUDIO_SAMPLE_RATE);
        updateFieldIfChanged(trackRecord, genre, TRACK.GENRE);
        updateFieldIfChanged(trackRecord, composer, TRACK.COMPOSER);
        updateFieldIfChanged(trackRecord, duration, TRACK.DURATION);
        updateFieldIfChanged(trackRecord, lyrics, TRACK.LYRICS);
        return trackRecord.changed();
    }

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
