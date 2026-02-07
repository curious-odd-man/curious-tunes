package com.github.curiousoddman.curious_tunes.backend.tags;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

class FilesScanningServiceTest {

    @Test
    void mp3Test() {
        MetadataManager filesScanningService = new MetadataManager();
        MetadataTags metadataTags = filesScanningService.getMetadata(Path.of("D:\\iTunes\\iTunes 1\\iTunes Media\\Music\\deti-online.com\\Логоритмика\\Вот мы в автобусе сидим.mp3"));
        System.out.println(metadataTags);
    }

}