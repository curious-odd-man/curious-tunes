package com.github.curiousoddman.curious_tunes.web;

import com.github.curiousoddman.alacdecoder.AlacDecoder;
import com.github.curiousoddman.alacdecoder.data.WavFormat;
import com.github.curiousoddman.curious_tunes.model.PlaylistModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HttpRestController {
    private final PlaylistModel playlistModel;

    @GetMapping(
            value = "/current/track",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    @Cacheable(value = "media", key = "#name")
    public @ResponseBody byte[] get(@RequestParam("name") String name) throws IOException {
        log.info("Requested {}", name);
        String fileLocation = playlistModel
                .getCurrentlyPlaying()
                .get()
                .getTrackRecord()
                .getFileLocation();
        if (!fileLocation.equals(name)) {
            log.error("Requested and served files are different");
        }
        Path path = Path.of(fileLocation).toAbsolutePath().normalize();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        AlacDecoder
                .decode(WavFormat.RAW_PCM)
                .fromFile(path)
                .toStream(baos)
                .execute();

        return baos.toByteArray();
    }
}
