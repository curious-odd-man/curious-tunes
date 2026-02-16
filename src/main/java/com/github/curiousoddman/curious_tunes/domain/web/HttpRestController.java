package com.github.curiousoddman.curious_tunes.domain.web;

import com.github.curiousoddman.alacdecoder.AlacDecoder;
import com.github.curiousoddman.alacdecoder.data.WavFormat;
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
    @GetMapping(
            value = "/current/track",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    @Cacheable(value = "media", key = "#name")
    public @ResponseBody byte[] get(@RequestParam("name") String name) throws IOException {
        log.info("Requested {}", name);
        Path path = Path.of(name).toAbsolutePath().normalize();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        AlacDecoder
                .decode(WavFormat.RAW_PCM)
                .fromFile(path)
                .toStream(baos)
                .execute();

        return baos.toByteArray();
    }
}
