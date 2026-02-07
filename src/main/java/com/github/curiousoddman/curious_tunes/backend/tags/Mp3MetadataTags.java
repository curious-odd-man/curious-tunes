package com.github.curiousoddman.curious_tunes.backend.tags;

import com.github.curiousoddman.curious_tunes.util.ConversionUtils;
import com.mpatric.mp3agic.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static com.github.curiousoddman.curious_tunes.backend.tags.AlbumCover.DataType.JPG;

@Slf4j
@Getter
public class Mp3MetadataTags extends MetadataTagsBase implements MetadataTags {
    private final Mp3File sourceFile;

    public Mp3MetadataTags(Mp3File mp3file, Path path) {
        super(path.toAbsolutePath().toString());
        this.sourceFile = mp3file;

        duration = mp3file.getLengthInSeconds();

        if (mp3file.hasId3v2Tag()) {
            ID3v2 tag = mp3file.getId3v2Tag();
            artist = tag.getArtist();
            album = tag.getAlbum();
            title = tag.getTitle();
            trackNumber = ConversionUtils.asInteger(tag.getTrack());
            genre = tag.getGenreDescription();
            lyrics = tag.getLyrics();
            albumCover = new AlbumCover(tag.getAlbumImage(), tag.getAlbumImageMimeType());
        } else if (mp3file.hasId3v1Tag()) {
            ID3v1 tag = mp3file.getId3v1Tag();
            artist = tag.getArtist();
            album = tag.getAlbum();
            title = tag.getTitle();
            trackNumber = ConversionUtils.asInteger(tag.getTrack());
            genre = tag.getGenreDescription();
        }

        verifyRequiredValuesPresent();
    }

    @Override
    public void updateFile() throws IOException {
        try {
            Path tempFile = Files.createTempFile("tmp-file", "mp3");
            sourceFile.save(tempFile.toAbsolutePath().toString());
            Files.move(tempFile, Path.of(fileLocation), StandardCopyOption.REPLACE_EXISTING);
        } catch (NotSupportedException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    protected void onArtistUpdated() {
        ID3v2 tag = getOrCreateId3v2Tag();
        tag.setArtist(artist);
    }

    @Override
    protected void onAlbumUpdated() {
        ID3v2 tag = getOrCreateId3v2Tag();
        tag.setAlbum(album);
    }

    @Override
    protected void onTitleUpdated() {
        ID3v2 tag = getOrCreateId3v2Tag();
        tag.setTitle(title);
    }

    @Override
    protected void onTrackNumberUpdated() {
        ID3v2 tag = getOrCreateId3v2Tag();
        tag.setTrack(trackNumber != null ? trackNumber.toString() : null);
    }

    @Override
    protected void onReleaseDateUpdated() {
        ID3v2 tag = getOrCreateId3v2Tag();
        tag.setYear(releaseDate);
    }

    @Override
    protected void onDiskNumberUpdated() {
        ID3v2 tag = getOrCreateId3v2Tag();
        tag.setPartOfSet(diskNumber != null ? diskNumber.toString() : null);
    }

    @Override
    protected void onGenreUpdated() {
        ID3v2 tag = getOrCreateId3v2Tag();
        tag.setGenreDescription(genre);
    }

    @Override
    protected void onComposerUpdated() {
        ID3v2 tag = getOrCreateId3v2Tag();
        tag.setComposer(composer);
    }

    @Override
    protected void onLyricsUpdated() {
        ID3v2 tag = getOrCreateId3v2Tag();
        tag.setLyrics(lyrics);
    }

    @Override
    protected void onAlbumCoverUpdated() {
        ID3v2 tag = getOrCreateId3v2Tag();

        if (albumCover != null && albumCover.getData() != null) {
            tag.setAlbumImage(
                    albumCover.getData(),
                    albumCover.getType().equals(JPG.toString()) ? "image/jpeg" : "image/png"
            );
        } else {
            tag.setAlbumImage(null, null);
        }
    }

    private ID3v2 getOrCreateId3v2Tag() {
        if (sourceFile.hasId3v2Tag()) {
            return sourceFile.getId3v2Tag();
        } else {
            ID3v2 tag = new ID3v24Tag();
            sourceFile.setId3v2Tag(tag);
            if (sourceFile.hasId3v1Tag()) {
                ID3v1 v1 = sourceFile.getId3v1Tag();

                tag.setArtist(v1.getArtist());
                tag.setAlbum(v1.getAlbum());
                tag.setTitle(v1.getTitle());
                tag.setTrack(v1.getTrack());
                tag.setYear(v1.getYear());
                tag.setGenre(v1.getGenre());
                tag.setComment(v1.getComment());
            }
            return tag;
        }
    }
}
