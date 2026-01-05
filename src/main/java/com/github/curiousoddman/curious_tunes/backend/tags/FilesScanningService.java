package com.github.curiousoddman.curious_tunes.backend.tags;

import com.github.curiousoddman.curious_tunes.backend.DataAccess;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.event.BackgroundProcessEvent;
import com.github.curiousoddman.curious_tunes.event.InterruptBackgroundProcessEvent;
import com.github.curiousoddman.curious_tunes.event.RescanLibraryEvent;
import com.github.curiousoddman.curious_tunes.event.types.BackgroundProcessEventType;
import com.github.curiousoddman.curious_tunes.model.TrackStatus;
import com.mpatric.mp3agic.Mp3File;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.mp4parser.Box;
import org.mp4parser.IsoFile;
import org.mp4parser.boxes.UnknownBox;
import org.mp4parser.boxes.iso14496.part12.FreeBox;
import org.mp4parser.support.AbstractBox;
import org.mp4parser.support.AbstractContainerBox;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@Slf4j
@Component
@RequiredArgsConstructor
public class FilesScanningService {
    private static final int APPROXIMATE_SIZE_OF_MY_LIBRARY = 5000;
    public static final String LIBRARY_SCAN = "Library Scan";
    private final ApplicationEventPublisher applicationEventPublisher;
    private final DataAccess dataAccess;

    private boolean shouldInterrupt;

    @EventListener
    public void onRescanEvent(RescanLibraryEvent event) {
        String libraryRoot = event.getPath();
        shouldInterrupt = false;
        log.info("Received rescan library event...");
        Runnable rescanRunnable = () -> {
            log.info("Starting scanning: discover files...");
            applicationEventPublisher.publishEvent(
                    getBackgroundProcessEventBuilder()
                            .eventType(BackgroundProcessEventType.STARTED)
                            .description("Discovering files...")
                            .maxProgress(-1)
                            .build());
            try {
                List<Path> paths = doScan(Path.of(libraryRoot));
                log.info("Discovered {} files. Started processing", paths.size());
                applicationEventPublisher.publishEvent(
                        getBackgroundProcessEventBuilder()
                                .eventType(BackgroundProcessEventType.IN_PROGRESS)
                                .maxProgress(paths.size())
                                .description("Fetching metadata...")
                                .build());
                for (int i = 0; i < paths.size(); i++) {
                    Path file = paths.get(i);
                    MDC.put("file", String.valueOf(i));
                    log.info("\t{}", file);
                    if (shouldInterrupt) {
                        log.info("Scanning interrupted");
                        applicationEventPublisher.publishEvent(
                                getBackgroundProcessEventBuilder()
                                        .eventType(BackgroundProcessEventType.INTERRUPTED)
                                        .description("Interrupted")
                                        .build());
                        return;
                    }

                    extractMetadataAndUpdateDatabase(file);
                    applicationEventPublisher.publishEvent(
                            getBackgroundProcessEventBuilder()
                                    .eventType(BackgroundProcessEventType.IN_PROGRESS)
                                    .progress(i + 1)
                                    .maxProgress(paths.size())
                                    .description("Fetching metadata...")
                                    .build());
                }
                applicationEventPublisher.publishEvent(
                        getBackgroundProcessEventBuilder()
                                .eventType(BackgroundProcessEventType.ENDED)
                                .description("Interrupted")
                                .build());
                log.info("Scanning completed...");
            } catch (Exception e) {
                applicationEventPublisher.publishEvent(
                        getBackgroundProcessEventBuilder()
                                .eventType(BackgroundProcessEventType.FAILED)
                                .description("Failed...")
                                .error(e)
                                .build());
                log.error("Failed parsing files...", e);
                MDC.remove("file");
            }
        };
        Thread rescanThread = new Thread(rescanRunnable, "rescan");
        rescanThread.start();
    }

    private BackgroundProcessEvent.BackgroundProcessEventBuilder getBackgroundProcessEventBuilder() {
        return BackgroundProcessEvent
                .builder()
                .source(this)
                .processName(LIBRARY_SCAN);
    }

    private void extractMetadataAndUpdateDatabase(Path file) {
        try {
            String pathString = file.toString();
            MetadataTags metadata;
            if (pathString.endsWith("m4a")) {
                metadata = extractM4aMetadata(file);
            } else if (pathString.endsWith("mp3")) {
                metadata = extractMp3Tags(file);
            } else {
                log.error("Unsupported extension: {}", pathString);
                return;
            }
            ArtistRecord artistRecord = dataAccess.getOrInsertArtist(metadata.getArtist());
            AlbumRecord albumRecord = dataAccess.getOrInsertAlbum(artistRecord.getId(), metadata.getAlbum(), metadata.getAlbumCover().getData());
            TrackRecord trackRecord = dataAccess.getTrack(albumRecord.getId(), metadata.getTitle());

            if (trackRecord == null) {
                TrackRecord mewTrackRecord = new TrackRecord(
                        null,
                        albumRecord.getId(),
                        metadata.getTitle(),
                        metadata.getTrackNumber(),
                        metadata.getReleaseDate(),
                        metadata.getDiskNumber(),
                        metadata.getSampleRate(),
                        metadata.getGenre(),
                        metadata.getComposer(),
                        metadata.getFileLocation(),
                        metadata.getDuration(),
                        TrackStatus.ACTIVE.name(),
                        metadata.getLyrics()
                );
                dataAccess.insertTrack(mewTrackRecord);
            } else {
                if (metadata.updateTrackIfChanged(trackRecord)) {
                    trackRecord.update();
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse file {}", file, e);
        }
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
    MetadataTags extractMp3Tags(Path file) {
        Mp3File mp3file = new Mp3File(file);
        return new MetadataTags(mp3file, file);
    }

    @SneakyThrows
    static MetadataTags extractM4aMetadata(Path file) {
        List<Box> resultBoxes = new ArrayList<>();
        try (FileInputStream fileInputStream = new FileInputStream(file.toFile())) {
            IsoFile isoFile = new IsoFile(fileInputStream.getChannel());
            Queue<Box> boxes = new LinkedList<>(isoFile.getBoxes());
            while (!boxes.isEmpty()) {
                Box box = boxes.remove();
                if (box instanceof AbstractContainerBox container) {
                    boxes.addAll(container.getBoxes());
                }
                if (box instanceof AbstractBox abstractBox) {
                    abstractBox.parseDetails();
                }
                resultBoxes.add(box);
            }
        }

        List<Box> allBoxes = resultBoxes
                .stream()
                .filter(rb -> !(rb instanceof FreeBox))
                .filter(rb -> !(rb instanceof UnknownBox))
                .toList();
        return new MetadataTags(allBoxes, file);
    }
}
