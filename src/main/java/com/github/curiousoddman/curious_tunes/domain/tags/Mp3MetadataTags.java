package com.github.curiousoddman.curious_tunes.domain.tags;

import com.github.curiousoddman.curious_tunes.util.ConversionUtils;
import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;

import java.nio.file.Path;

public class Mp3MetadataTags extends MetadataTagsBase {
    public Mp3MetadataTags(Mp3File mp3file, Path path) {
        super(path.toAbsolutePath().toString());

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
}
