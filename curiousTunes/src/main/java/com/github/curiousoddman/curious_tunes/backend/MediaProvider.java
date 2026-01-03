package com.github.curiousoddman.curious_tunes.backend;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import javafx.scene.media.Media;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class MediaProvider {
    @Value("${server.port}")
    private final String serverPort;

    /**
     * This is intorduced because Media cannot be ALAC file + it cannot be created from InputStream - only a URI
     * Therefore all this fuss with a controller...
     * // FIXME:
     * // <a href="https://stackoverflow.com/questions/78908102/how-can-i-create-a-media-object-in-javafx-from-a-byte-array">I used this</a>
     * // Or, should I try and fix it in <a href="https://github.com/openjdk/jfx">Link to JavaFX source code</a>
     *
     * @param trackRecord track to be played
     * @return Media
     */
    public Media getMedia(TrackRecord trackRecord) {
        String fileLocation = trackRecord.getFileLocation();
        URI fileUri;

        if (fileLocation.toLowerCase(Locale.ROOT).endsWith("m4a")) {
            fileUri = URI.create("http://localhost:" + serverPort + "/current/track?name=" + URLEncoder.encode(fileLocation, StandardCharsets.UTF_8));
        } else {
            fileUri = Path.of(fileLocation).toUri();
        }

        return new Media(fileUri.toString());
    }
}
