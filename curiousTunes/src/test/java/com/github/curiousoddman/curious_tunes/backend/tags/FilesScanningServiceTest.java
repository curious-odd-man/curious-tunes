package com.github.curiousoddman.curious_tunes.backend.tags;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FilesScanningServiceTest {

    @Test
    void mp3Test() {
        FilesScanningService filesScanningService = new FilesScanningService(null, null);
        MetadataTags metadataTags = filesScanningService.extractMp3Tags(Path.of("D:\\iTunes\\iTunes 1\\iTunes Media\\Music\\deti-online.com\\Логоритмика\\Вот мы в автобусе сидим.mp3"));
        System.out.println(metadataTags);
    }

}