package com.github.curiousoddman.curious_tunes;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class Manual {
    public static void main(String[] args) {
        scrapeAllAudioFilesForTagsTest();
    }

    @SneakyThrows
    private static void scrapeAllAudioFilesForTagsTest() {
        Path resultPath = Path.of("parsing-result.txt");
        Map<String, Integer> stats = new HashMap<>();
        // Walk directory tree
        Files.walkFileTree(Path.of("D:\\iTunes\\iTunes 1\\iTunes Media\\Music"), new SimpleFileVisitor<>() {
            private int i = 1;

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                System.out.print('.');
                if (i++ % 150 == 0) {
                    System.out.println();
                }
                try {
                    Map<String, String> tags = extractMetadata(file);
                    StringBuilder sb = new StringBuilder("File: ").append(file.toAbsolutePath()).append('\n');
                    tags.forEach((k, v) -> {
                        stats.compute(k, (_, vv) -> vv == null ? 1 : vv + 1);
                        sb.append('\t').append(k).append('\t').append(v).append('\n');
                    });
                    Files.writeString(resultPath, sb.toString(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        StringBuilder sb = new StringBuilder();
        stats.forEach((k, v) -> {
            sb.append(k).append('\t').append(v).append('\n');
        });
        Files.writeString(Path.of("tag-stats.txt"), sb.toString(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }


    private static Map<String, String> extractMetadata(Path file) throws IOException {
        Tika tika = new Tika();
        Metadata metadata = new Metadata();
        try (InputStream fis = Files.newInputStream(file)) {
            tika.parseToString(fis, metadata);
        } catch (TikaException e) {
            log.error(e.getMessage(), e);
        }
        Map<String, String> tags = new HashMap<>();
        String[] names = metadata.names();
        for (String name : names) {
            tags.put(name, metadata.get(name));
        }
        return tags;
    }
}
