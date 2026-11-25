package com.github.curiousoddman.curious_tunes.domain.tags;

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
}
