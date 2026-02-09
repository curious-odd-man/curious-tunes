package com.github.curiousoddman.curious_tunes.backend.tags;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.mp4parser.Box;
import org.mp4parser.IsoFile;
import org.mp4parser.boxes.apple.*;
import org.mp4parser.boxes.iso14496.part12.MediaHeaderBox;
import org.mp4parser.boxes.iso14496.part12.TrackHeaderBox;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

@Slf4j
@Getter
public class Mp4MetadataTags extends MetadataTagsBase implements MetadataTags {
    private final IsoFile isoFile;
    private AppleNameBox appleNameBox;
    private AppleLyricsBox appleLyricsBox;
    private AppleTrackNumberBox appleTrackNumberBox;
    private AppleArtistBox appleArtistBox;
    private AppleCoverBox appleCoverBox;
    private AppleTrackAuthorBox appleTrackAuthorBox;
    private AppleDiskNumberBox appleDiskNumberBox;
    private AppleGenreBox appleGenreBox;
    private AppleAlbumBox appleAlbumBox;
    private AppleRecordingYear2Box appleRecordingYear2Box;


    public Mp4MetadataTags(IsoFile isoFile, List<Box> allBoxes, Path path) {
        super(path.toAbsolutePath().toString());
        this.isoFile = isoFile;
        for (Box box : allBoxes) {
            switch (box) {
                case AppleNameBox nameBox -> {
                    this.appleNameBox = nameBox;
                    title = validate("title", title, nameBox.getValue());
                }
                case AppleLyricsBox lyricsBox -> {
                    this.appleLyricsBox = lyricsBox;
                    lyrics = validate("lyrics", lyrics, lyricsBox.getValue());
                }
                case AppleTrackNumberBox trackNumberBox -> {
                    this.appleTrackNumberBox = trackNumberBox;
                    trackNumber = validate("trackNumber 1", trackNumber, trackNumberBox.getA());
                }
                case AppleArtistBox artistBox -> {
                    appleArtistBox = artistBox;
                    artist = validate("artist", artist, artistBox.getValue());
                }
                case AppleArtist2Box albumArtistBox -> {
                    //Album artist, if I ever need it
                }
                case AppleCoverBox coverBox -> {
                    appleCoverBox = coverBox;
                    albumCover = validate("albumCover", albumCover, new AlbumCover(coverBox.getCoverData(), coverDataType(coverBox)));
                }
                case AppleTrackAuthorBox trackAuthorBox -> {
                    appleTrackAuthorBox = trackAuthorBox;
                    composer = validate("composer", composer, trackAuthorBox.getValue());
                }
                case AppleDiskNumberBox diskNumberBox -> {
                    appleDiskNumberBox = diskNumberBox;
                    diskNumber = validate("diskNumber", diskNumber, diskNumberBox.getA());
                }
                case TrackHeaderBox headerBox -> {
                    //  duration = validate("duration 1", duration, headerBox.getDuration());      // SO suggests that duration can be calculated from MediaHeaderBox
                    // trackNumber = validate("trackNumber 2", trackNumber, (int) headerBox.getTrackId()); // This is not valid value, as we have found in Apple files
                }
                case AppleGenreBox genreBox -> {
                    appleGenreBox = genreBox;
                    genre = validate("genre", genre, genreBox.getValue());
                }
                case AppleAlbumBox albumBox -> {
                    appleAlbumBox = albumBox;
                    album = validate("album", album, albumBox.getValue());
                }
                case MediaHeaderBox mediaHeaderBox -> {
                    sampleRate = validate("sampleRate", sampleRate, (int) mediaHeaderBox.getTimescale());
                    duration = validate("duration 2", duration, mediaHeaderBox.getDuration() / mediaHeaderBox.getTimescale());
                }
                case AppleRecordingYear2Box recordingYear2Box -> {
                    appleRecordingYear2Box = recordingYear2Box;
                    releaseDate = validate("releaseDate", releaseDate, recordingYear2Box.getValue());
                }
                default -> {
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

    @Override
    public void updateFile() throws IOException {
        try (WritableByteChannel channel = Files.newByteChannel(Path.of(fileLocation), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            isoFile.getBox(channel);
        }
    }

    @Override
    protected void onArtistUpdated() {
        appleArtistBox.setValue(artist);
    }

    @Override
    protected void onAlbumUpdated() {
        appleAlbumBox.setValue(album);
    }

    @Override
    protected void onTitleUpdated() {
        appleNameBox.setValue(title);
    }

    @Override
    protected void onTrackNumberUpdated() {
        appleTrackNumberBox.setA(trackNumber);
    }

    @Override
    protected void onReleaseDateUpdated() {
        appleRecordingYear2Box.setValue(releaseDate);
    }

    @Override
    protected void onDiskNumberUpdated() {
        appleDiskNumberBox.setA(diskNumber);
    }

    @Override
    protected void onGenreUpdated() {
        appleGenreBox.setValue(genre);
    }

    @Override
    protected void onComposerUpdated() {
        // FIXME: what if it is not defined????
        appleTrackAuthorBox.setValue(composer);
    }

    @Override
    protected void onLyricsUpdated() {
        appleLyricsBox.setValue(lyrics);
    }

    @Override
    protected void onAlbumCoverUpdated() {
        switch (albumCover.getType()) {
            case "PNG":
                appleCoverBox.setPng(albumCover.getData());
                break;
            case "JPG":
                appleCoverBox.setJpg(albumCover.getData());
                break;
            default:
                throw new IllegalStateException("Cannot update cover - unknown data type: " + albumCover.getType());
        }
    }
}
