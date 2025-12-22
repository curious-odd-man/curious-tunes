package com.github.curiousoddman.curious_tunes.backend.tags;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.util.JooqUtils;
import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.mp4parser.Box;
import org.mp4parser.boxes.apple.*;
import org.mp4parser.boxes.iso14496.part12.MediaHeaderBox;
import org.mp4parser.boxes.iso14496.part12.TrackHeaderBox;

import java.nio.file.Path;
import java.util.ArrayList;
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
        extractM4aTags(allBoxes);
        verifyRequiredValuesPresent();
    }

    public MetadataTags(Mp3File mp3file, Path path) {
        fileLocation = path.toAbsolutePath().toString();
        duration = mp3file.getLengthInSeconds();

        if (mp3file.hasId3v2Tag()) {
            ID3v2 tag = mp3file.getId3v2Tag();
            artist = tag.getArtist();
            album = tag.getAlbum();
            title = tag.getTitle();
            trackNumber = Integer.valueOf(tag.getTrack());
            genre = tag.getGenreDescription();
            lyrics = tag.getLyrics();
            albumCover = new AlbumCover(tag.getAlbumImage(), tag.getAlbumImageMimeType());
        } else if (mp3file.hasId3v1Tag()) {
            ID3v1 tag = mp3file.getId3v1Tag();
            artist = tag.getArtist();
            album = tag.getAlbum();
            title = tag.getTitle();
            trackNumber = Integer.valueOf(tag.getTrack());
            genre = tag.getGenreDescription();
        }

        verifyRequiredValuesPresent();
    }

    private void extractM4aTags(List<Box> allBoxes) {
        for (Box box : allBoxes) {
            switch (box) {
                case AppleNameBox appleNameBox -> title = validate("title", title, appleNameBox.getValue());
                case AppleLyricsBox appleLyricsBox -> lyrics = validate("lyrics", lyrics, appleLyricsBox.getValue());
                case AppleTrackNumberBox appleTrackNumberBox ->
                        trackNumber = validate("trackNumber 1", trackNumber, appleTrackNumberBox.getA());
                case AppleArtistBox appleArtistBox -> artist = validate("artist", artist, appleArtistBox.getValue());
                case AppleArtist2Box appleArtist2Box -> {
                    //Album artist, if I ever need it
                }
                case AppleCoverBox appleCoverBox ->
                        albumCover = validate("albumCover", albumCover, new AlbumCover(appleCoverBox.getCoverData(), coverDataType(appleCoverBox)));
                case AppleTrackAuthorBox appleTrackAuthorBox ->
                        composer = validate("composer", composer, appleTrackAuthorBox.getValue());
                case AppleDiskNumberBox appleDiskNumberBox ->
                        diskNumber = validate("diskNumber", diskNumber, appleDiskNumberBox.getA());
                case TrackHeaderBox trackHeaderBox -> {
                    //  duration = validate("duration 1", duration, trackHeaderBox.getDuration());      // SO suggests that duration can be calculated from MediaHeaderBox
                    // trackNumber = validate("trackNumber 2", trackNumber, (int) trackHeaderBox.getTrackId()); // This is not valid value, as we have found in Apple files
                }
                case AppleGenreBox appleGenreBox -> genre = validate("genre", genre, appleGenreBox.getValue());
                case AppleAlbumBox appleAlbumBox -> album = validate("album", album, appleAlbumBox.getValue());
                case MediaHeaderBox mediaHeaderBox -> {
                    sampleRate = validate("sampleRate", sampleRate, (int) mediaHeaderBox.getTimescale());
                    duration = validate("duration 2", duration, mediaHeaderBox.getDuration() / mediaHeaderBox.getTimescale());
                }
                case AppleRecordingYear2Box appleRecordingYear2Box ->
                        releaseDate = validate("releaseDate", releaseDate, appleRecordingYear2Box.getValue());
                default -> {
                }
            }
        }
    }

    private void verifyRequiredValuesPresent() {
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

    private static String coverDataType(AppleCoverBox appleCoverBox) {
        AlbumCover.DataType dataType = switch (appleCoverBox.getDataType()) {
            case 13 -> AlbumCover.DataType.JPG;
            case 14 -> AlbumCover.DataType.PNG;
            default -> {
                log.error("Unkonwn data type: {}", appleCoverBox.getDataType());
                yield null;
            }
        };
        return dataType == null ? null : dataType.toString();
    }

    public <T> T validate(String description, T field, T newValue) {
        if (field == null) {
            return newValue;
        }
        if (!Objects.equals(field, newValue)) {
            log.info("Different value duplicate box detected {} --> {}:{}", description, field, newValue);
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
