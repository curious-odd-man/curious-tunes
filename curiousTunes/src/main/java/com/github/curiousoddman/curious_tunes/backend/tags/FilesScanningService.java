package com.github.curiousoddman.curious_tunes.backend.tags;

import com.github.curiousoddman.curious_tunes.backend.DataAccess;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.event.BackgroundProcessEndedEvent;
import com.github.curiousoddman.curious_tunes.event.BackgroundProcessEvent;
import com.github.curiousoddman.curious_tunes.event.InterruptBackgroundProcessEvent;
import com.github.curiousoddman.curious_tunes.event.RescanLibraryEvent;
import com.github.curiousoddman.curious_tunes.model.TrackStatus;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.mp4parser.Box;
import org.mp4parser.IsoFile;
import org.mp4parser.boxes.UnknownBox;
import org.mp4parser.boxes.iso14496.part12.FreeBox;
import org.mp4parser.support.AbstractBox;
import org.mp4parser.support.AbstractContainerBox;
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
            applicationEventPublisher.publishEvent(new BackgroundProcessEvent(this, "Discovering files...", 0, -1));
            List<Path> paths = doScan(Path.of(libraryRoot));
            log.info("Discovered {} files. Started processing", paths.size());
            applicationEventPublisher.publishEvent(new BackgroundProcessEvent(this, "Processing files...", 0, paths.size()));
            for (int i = 0; i < paths.size(); i++) {
                Path file = paths.get(i);
                if (shouldInterrupt) {
                    log.info("Scanning interrupted");
                    applicationEventPublisher.publishEvent(new BackgroundProcessEndedEvent(this, "Scanning interrupted", null));
                    return;
                }

                extractMetadataAndUpdateDatabase(file);
                applicationEventPublisher.publishEvent(new BackgroundProcessEvent(this, "Processing files...", i + 1, paths.size()));
            }
            applicationEventPublisher.publishEvent(new BackgroundProcessEndedEvent(this, "Scanning interrupted", null));
            log.info("Scanning completed...");
        };
        Thread rescanThread = new Thread(rescanRunnable, "rescan");
        rescanThread.start();
    }

    private void extractMetadataAndUpdateDatabase(Path file) {
        MetadataTags metadata = extractMetadata(file);
        ArtistRecord artistRecord = dataAccess.getOrInsertArtist(metadata.getArtist());
        AlbumRecord albumRecord = dataAccess.getOrInsertAlbum(artistRecord.getId(), metadata.getAlbum());
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
    private static MetadataTags extractMetadata(Path file) {
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
