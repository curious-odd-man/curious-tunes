package com.github.curiousoddman.curious_tunes.domain.tags;

import java.io.IOException;
import java.nio.file.Path;

public class Mp4FailedParsingTags extends MetadataTagsBase {
    public Mp4FailedParsingTags(String fileLocation, String errorText) {
        super(fileLocation);
        artist = "Failed Parsing Artist";
        Path path = Path.of(fileLocation);
        String fileName = path.getFileName().toString();
        String albumDir = path.getParent().getFileName().toString();
        String artistDir = path.getParent().getParent().getFileName().toString();

        album = artistDir + " / " + albumDir;
        title = fileName;

        lyrics = errorText;
    }

    @Override
    protected void onArtistUpdated() {

    }

    @Override
    protected void onAlbumUpdated() {

    }

    @Override
    protected void onTitleUpdated() {

    }

    @Override
    protected void onTrackNumberUpdated() {

    }

    @Override
    protected void onReleaseDateUpdated() {

    }

    @Override
    protected void onDiskNumberUpdated() {

    }

    @Override
    protected void onGenreUpdated() {

    }

    @Override
    protected void onComposerUpdated() {

    }

    @Override
    protected void onLyricsUpdated() {

    }

    @Override
    protected void onAlbumCoverUpdated() {

    }

    @Override
    public void updateFile() throws IOException {

    }
}
