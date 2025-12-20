package com.github.curiousoddman.curious_tunes.backend.tags;

import com.github.curiousoddman.curious_tunes.event.BackgroundProcessEndedEvent;
import com.github.curiousoddman.curious_tunes.event.BackgroundProcessEvent;
import com.github.curiousoddman.curious_tunes.event.InterruptBackgroundProcessEvent;
import com.github.curiousoddman.curious_tunes.event.RescanLibraryEvent;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class FilesScanningService {
    private static final int APPROXIMATE_SIZE_OF_MY_LIBRARY = 5000;
    private final ApplicationEventPublisher applicationEventPublisher;

    private boolean shouldInterrupt;

    @EventListener
    public void onRescanEvent(RescanLibraryEvent event) {
        String libraryRoot = event.getPath();
        shouldInterrupt = false;
        log.info("Received rescan library event...");
        Runnable rescanRunnable = () -> {
            log.info("Starting scanning: discover files...");
            applicationEventPublisher.publishEvent(new BackgroundProcessEvent(this, "Discovering files...", 0, -1));
            List<Path> paths = doScan(Path.of(libraryRoot));
            log.info("Discovered {} files. Started processing", paths.size());
            applicationEventPublisher.publishEvent(new BackgroundProcessEvent(this, "Processing files...", 0, paths.size()));
            for (Path file : paths) {
                if (shouldInterrupt) {
                    log.info("Scanning interrupted");
                    applicationEventPublisher.publishEvent(new BackgroundProcessEndedEvent(this, "Scanning interrupted", null));
                    return;
                }

                Map<String, String> metadata = extractMetadata(file);
                String artist = metadata.get("xmpDM:artist");

            }
            applicationEventPublisher.publishEvent(new BackgroundProcessEndedEvent(this, "Scanning interrupted", null));
            log.info("Scanning completed...");
        };
        Thread rescanThread = new Thread(rescanRunnable, "rescan");
        rescanThread.start();
    }

    @EventListener
    public void onInterruptBackgroundProcess(InterruptBackgroundProcessEvent event) {
        shouldInterrupt = true;
    }

    @SneakyThrows
    private List<Path> doScan(Path path) {
        List<Path> foundFiles = new ArrayList<>(APPROXIMATE_SIZE_OF_MY_LIBRARY);
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (attrs.isRegularFile()) {
                    foundFiles.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return foundFiles;
    }

    @SneakyThrows
    private static Map<String, String> extractMetadata(Path file) {
        Tika tika = new Tika();
        Metadata metadata = new Metadata();
        try (InputStream fis = Files.newInputStream(file)) {
            tika.parseToString(fis, metadata);
        }
        Map<String, String> tags = new HashMap<>();
        String[] names = metadata.names();
        for (String name : names) {
            tags.put(name, metadata.get(name));
        }
        return tags;
    }
}
