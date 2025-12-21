package com.github.curiousoddman.curious_tunes.backend.tags;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.util.JooqUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.mp4parser.Box;
import org.mp4parser.boxes.apple.*;
import org.mp4parser.boxes.iso14496.part12.MediaHeaderBox;
import org.mp4parser.boxes.iso14496.part12.TrackHeaderBox;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static com.github.curiousoddman.curious_tunes.dbobj.tables.Track.TRACK;

@Slf4j
@Getter
public class MetadataTags {
    private final String fileLocation;

    private String artist;
    private String album;
    private String title;
    private Integer trackNumber;
    private String releaseDate;
    private Integer diskNumber;
    private Integer sampleRate;
    private String genre;
    private String composer;
    private Long duration;
    private String lyrics;
    private AlbumCover albumCover;

    public MetadataTags(List<Box> allBoxes, Path path) {
        fileLocation = path.toAbsolutePath().toString();

        for (Box box : allBoxes) {
            switch (box) {
                case AppleNameBox appleNameBox -> title = validate(title, appleNameBox.getValue());
                case AppleLyricsBox appleLyricsBox -> lyrics = validate(lyrics, appleLyricsBox.getValue());
                case AppleTrackNumberBox appleTrackNumberBox ->
                        trackNumber = validate(trackNumber, appleTrackNumberBox.getA());
                case AppleArtist2Box appleArtist2Box -> artist = validate(artist, appleArtist2Box.getValue());
                case AppleCoverBox appleCoverBox ->
                        albumCover = validate(albumCover, new AlbumCover(appleCoverBox.getCoverData(), coverDataType(appleCoverBox)));
                case AppleTrackAuthorBox appleTrackAuthorBox ->
                        composer = validate(composer, appleTrackAuthorBox.getValue());
                case AppleDiskNumberBox appleDiskNumberBox ->
                        diskNumber = validate(diskNumber, appleDiskNumberBox.getA());
                case TrackHeaderBox trackHeaderBox -> {
                    duration = validate(duration, trackHeaderBox.getDuration());
                    trackNumber = validate(trackNumber, (int) trackHeaderBox.getTrackId());
                }
                case AppleGenreBox appleGenreBox -> genre = validate(genre, appleGenreBox.getValue());
                case AppleAlbumBox appleAlbumBox -> album = validate(album, appleAlbumBox.getValue());
                case MediaHeaderBox mediaHeaderBox -> {
                    sampleRate = validate(sampleRate, (int) mediaHeaderBox.getTimescale());
                    duration = validate(duration, mediaHeaderBox.getDuration());
                }
                case AppleRecordingYear2Box appleRecordingYear2Box ->
                        releaseDate = validate(releaseDate, appleRecordingYear2Box.getValue());
                default -> {
                }
            }
        }
    }

    private static AlbumCover.DataType coverDataType(AppleCoverBox appleCoverBox) {
        return switch (appleCoverBox.getDataType()) {
            case 13 -> AlbumCover.DataType.JPG;
            case 14 -> AlbumCover.DataType.PNG;
            default -> {
                log.error("Unkonwn data type: {}", appleCoverBox.getDataType());
                yield null;
            }
        };
    }

    public <T> T validate(T field, T newValue) {
        if (field == null) {
            return newValue;
        }
        if (Objects.equals(field, newValue)) {
            log.info("Different value duplicate box detected {}:{}", field, newValue);
        }
        return newValue;
    }

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
}
