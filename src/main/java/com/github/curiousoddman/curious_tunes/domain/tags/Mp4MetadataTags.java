package com.github.curiousoddman.curious_tunes.domain.tags;

import lombok.extern.slf4j.Slf4j;
import org.mp4parser.Box;
import org.mp4parser.boxes.apple.*;
import org.mp4parser.boxes.iso14496.part12.MediaHeaderBox;

import java.nio.file.Path;
import java.util.List;

@Slf4j
public class Mp4MetadataTags extends MetadataTagsBase {
    public Mp4MetadataTags(List<Box> allBoxes, Path path) {
        super(path.toAbsolutePath().toString());
        for (Box box : allBoxes) {
            switch (box) {
                case AppleNameBox nameBox -> {
                    title = validate("title", title, nameBox.getValue());
                }
                case AppleLyricsBox lyricsBox -> {
                    lyrics = validate("lyrics", lyrics, lyricsBox.getValue());
                }
                case AppleTrackNumberBox trackNumberBox -> {
                    trackNumber = validate("trackNumber 1", trackNumber, trackNumberBox.getA());
                }
                case AppleArtistBox artistBox -> {
                    artist = validate("artist", artist, artistBox.getValue());
                }
                case AppleCoverBox coverBox -> {
                    albumCover = validate("albumCover", albumCover, new AlbumCover(coverBox.getCoverData(), coverDataType(coverBox)));
                }
                case AppleTrackAuthorBox trackAuthorBox -> {
                    composer = validate("composer", composer, trackAuthorBox.getValue());
                }
                case AppleDiskNumberBox diskNumberBox -> {
                    diskNumber = validate("diskNumber", diskNumber, diskNumberBox.getA());
                }
                case AppleGenreBox genreBox -> {
                    genre = validate("genre", genre, genreBox.getValue());
                }
                case AppleAlbumBox albumBox -> {
                    album = validate("album", album, albumBox.getValue());
                }
                case MediaHeaderBox mediaHeaderBox -> {
                    sampleRate = validate("sampleRate", sampleRate, (int) mediaHeaderBox.getTimescale());
                    duration = validate("duration 2", duration, mediaHeaderBox.getDuration() / mediaHeaderBox.getTimescale());
                }
                case AppleRecordingYear2Box recordingYear2Box -> {
                    releaseDate = validate("releaseDate", releaseDate, recordingYear2Box.getValue());
                }
                default -> {
                    // Not read: album artist (AppleArtist2Box); TrackHeaderBox, whose duration and track id are not
                    // reliable in Apple files (the duration comes from MediaHeaderBox instead).
                }
            }
        }
        verifyRequiredValuesPresent();
    }

    private static String coverDataType(AppleCoverBox appleCoverBox) {
        AlbumCover.DataType dataType = switch (appleCoverBox.getDataType()) {
            case 13 -> AlbumCover.DataType.JPG;
            case 14 -> AlbumCover.DataType.PNG;
            default -> {
                log.error("Unknown image data type: {}", appleCoverBox.getDataType());
                yield null;
            }
        };
        return dataType == null ? null : dataType.toString();
    }
}
