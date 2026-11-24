package com.github.curiousoddman.curious_tunes.domain.tags;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.domain.AudioFormat;
import com.github.curiousoddman.curious_tunes.domain.DataAccess;
import com.github.curiousoddman.curious_tunes.domain.FileStamp;
import com.github.curiousoddman.curious_tunes.domain.lyrics.cleanup.CleanerResult;
import com.github.curiousoddman.curious_tunes.domain.lyrics.cleanup.LyricsCleaner;
import com.github.curiousoddman.curious_tunes.event.BackgroundProcessEvent;
import com.github.curiousoddman.curious_tunes.event.InterruptBackgroundProcessEvent;
import com.github.curiousoddman.curious_tunes.event.RescanLibraryEvent;
import com.github.curiousoddman.curious_tunes.event.types.BackgroundProcessEventType;
import com.github.curiousoddman.curious_tunes.model.TrackStatus;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.github.curiousoddman.curious_tunes.dbobj.Tables.TRACK;
import static com.github.curiousoddman.curious_tunes.util.FileUtils.audioMd5;

@Slf4j
@Component
@RequiredArgsConstructor
public class FilesScanningService /*implements StartupRunnable*/ {
    private static final int                       APPROXIMATE_SIZE_OF_MY_LIBRARY = 5000;
    public static final  String                    LIBRARY_SCAN                   = "Library Scan";
    /**
     * Files per database transaction: one commit per file made the scan wait on the disk thousands of times.
     */
    private static final int                       BATCH_SIZE                     = 200;
    private static final long                      PROGRESS_INTERVAL_NANOS        = 100_000_000L;
    private final        ApplicationEventPublisher applicationEventPublisher;
    private final        DataAccess                dataAccess;
    private final        MetadataManager           metadataManager;
    private final        LyricsCleaner             lyricsCleaner;
    private final        TransactionTemplate       transactionTemplate;

    private long lastProgressNanos;

    /**
     * Set by the event listener thread, read by the scan thread: volatile, so a Stop request is always seen.
     */
    private volatile boolean shouldInterrupt;

    @EventListener
    public void onRescanEvent(RescanLibraryEvent event) {
        String libraryRoot = event.getPath();
        shouldInterrupt = false;
        log.info("Received rescan library event...");
        boolean full = event.isFull();
        Runnable rescanRunnable = () -> {
            log.info("Starting scanning: discover files...");
            publishStartedEvent();
            try {
                List<ScannedFile> files = doScan(Path.of(libraryRoot));
                log.info("Discovered {} files. Started processing{}", files.size(), full ? " (reading every file)" : "");
                publishInProgressEvent(files.size());
                scanFiles(files, full);
            } catch (Exception e) {
                publishFailedEvent(e);
                log.error("Failed parsing files...", e);
            } finally {
                MDC.remove("file");
            }
        };
        Thread rescanThread = new Thread(rescanRunnable, "rescan");
        rescanThread.start();
    }

    /**
     * Files are processed in batches, each in one database transaction (item 5.2). A file whose size and
     * modification time match the last scan is not read at all (item 5.1), unless {@code full}.
     */
    private void scanFiles(List<ScannedFile> files, boolean full) {
        Map<String, FileStamp> stamps    = dataAccess.getTrackFileStamps();
        ScanCache              cache     = new ScanCache();
        int[]                  counts    = new int[4];      // added, updated, unchanged, failed
        int                    processed = 0;
        while (processed < files.size()) {
            if (shouldInterrupt) {
                log.info("Scanning interrupted");
                publishInterruptedEvent();
                return;
            }
            int from = processed;
            int to   = Math.min(files.size(), from + BATCH_SIZE);
            transactionTemplate.executeWithoutResult(status -> {
                for (int i = from; i < to && !shouldInterrupt; i++) {
                    scanFile(files.get(i), i, full, stamps, cache, counts);
                    publishProgressThrottled(i + 1, files.size());
                }
            });
            processed = to;
        }
        publishCompletedEvent(counts[0], counts[1], counts[2], counts[3]);
        log.info("Scanning completed: {} added, {} updated, {} unchanged, {} failed", counts[0], counts[1], counts[2], counts[3]);
    }

    private void scanFile(ScannedFile scanned, int index, boolean full, Map<String, FileStamp> stamps, ScanCache cache,
                          int[] counts) {
        Path file = scanned.path();
        MDC.put("file", String.valueOf(index));
        log.debug("\t{}", file);
        FileStamp stored = stamps.get(file.toAbsolutePath()
                                          .toString());      // locations are stored absolute
        if (!full && stored != null && stored.matches(scanned.size(), scanned.modifiedMillis())) {
            counts[2]++;                                // unchanged since the last scan: not read
            return;
        }
        try {
            switch (extractMetadataAndUpdateDatabase(scanned, cache)) {
                case ADDED -> counts[0]++;
                case UPDATED -> counts[1]++;
                case UNCHANGED -> counts[2]++;
            }
        } catch (Exception e) {
            // One unreadable file must not stop the scan of everything after it.
            counts[3]++;
            log.warn("Skipped {}: {}", file, e.toString(), e);
        }
    }

    private enum ScanOutcome {
        ADDED, UPDATED, UNCHANGED
    }

    /**
     * Artists and albums already looked up during one scan, so the tracks of one album don't look the album up again.
     * Lives for one scan only, so it can never return records from before a tag edit.
     */
    private static final class ScanCache {
        private record AlbumKey(int artistId, String name) {
        }

        private final Map<String, ArtistRecord> artists = new HashMap<>();
        private final Map<AlbumKey, AlbumRecord> albums  = new HashMap<>();
    }

    private ScanOutcome extractMetadataAndUpdateDatabase(ScannedFile scanned, ScanCache cache) {
        Path         file         = scanned.path();
        MetadataTags metadata     = metadataManager.getMetadata(file);
        ArtistRecord artistRecord = cache.artists.computeIfAbsent(metadata.getArtist(), dataAccess::getOrInsertArtist);
        AlbumCover   albumCover   = metadata.getAlbumCover();
        AlbumRecord  albumRecord  = cache.albums.computeIfAbsent(
                new ScanCache.AlbumKey(artistRecord.getId(), metadata.getAlbum()),
                key -> dataAccess.getOrInsertAlbum(key.artistId(), key.name(), albumCover == null ? null : albumCover.getData()));
        Optional<TrackRecord> trackRecord = dataAccess.getTrack(file);

        TrackRecord newTrackRecord;
        ScanOutcome outcome;
        if (trackRecord.isEmpty()) {
            newTrackRecord = new TrackRecord(
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
                    audioMd5(Path.of(metadata.getFileLocation())),
                    LocalDateTime.now(),
                    metadata.getDuration(),
                    TrackStatus.ACTIVE.name(),
                    metadata.getLyrics(),
                    scanned.size(),
                    scanned.modifiedMillis()
            );
            newTrackRecord = dataAccess.insertTrack(newTrackRecord);
            outcome = ScanOutcome.ADDED;
        } else {
            newTrackRecord = trackRecord.get();
            outcome = ScanOutcome.UNCHANGED;
            boolean changed = metadata.updateTrackIfChanged(newTrackRecord, albumRecord.getId());
            // Remember the file's size and time, so it isn't read again until it changes.
            newTrackRecord.setFileSize(scanned.size());
            newTrackRecord.setFileModified(scanned.modifiedMillis());
            newTrackRecord.update();                    // stores only what changed
            if (changed) {
                outcome = ScanOutcome.UPDATED;
            }
        }
        if (autoFixLyrics(newTrackRecord) && outcome == ScanOutcome.UNCHANGED) {
            outcome = ScanOutcome.UPDATED;
        }
        return outcome;
    }

    /**
     * @return true when the lyrics were changed
     */
    private boolean autoFixLyrics(TrackRecord trackRecord) {
        String        lyrics        = trackRecord.getLyrics();
        CleanerResult cleanerResult = lyricsCleaner.clean(lyrics);
        if (cleanerResult.wasModified()) {
            dataAccess.storeTrackOverride(trackRecord, TRACK.LYRICS, lyrics);
            trackRecord.setLyrics(cleanerResult.cleaned());
            trackRecord.update(TRACK.LYRICS);
            return true;
        }
        return false;
    }

    @EventListener
    public void onInterruptBackgroundProcess(InterruptBackgroundProcessEvent event) {
        shouldInterrupt = true;
    }

    /**
     * An audio file found on disk, with the size and time the scan compares with the last scan.
     */
    private record ScannedFile(Path path, long size, long modifiedMillis) {
    }

    @SneakyThrows
    private List<ScannedFile> doScan(Path path) {
        List<ScannedFile> foundFiles = new ArrayList<>(APPROXIMATE_SIZE_OF_MY_LIBRARY);
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (attrs.isRegularFile() && AudioFormat.isAudioFile(file)) {      // skip covers, playlists, etc.
                    foundFiles.add(new ScannedFile(file, attrs.size(), attrs.lastModifiedTime()
                                                                             .toMillis()));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, java.io.IOException e) {
                log.warn("Cannot read {}: {}", file, e.toString());   // e.g. no permission: skip it, go on
                return FileVisitResult.CONTINUE;
            }
        });
        return foundFiles;
    }

    private void publishFailedEvent(Exception e) {
        applicationEventPublisher.publishEvent(
                getBackgroundProcessEventBuilder()
                        .eventType(BackgroundProcessEventType.FAILED)
                        .description("Failed...")
                        .error(e)
                        .build());
    }

    private void publishCompletedEvent(int added, int updated, int unchanged, int failed) {
        String description = "%d added, %d updated, %d unchanged".formatted(added, updated, unchanged);
        if (failed > 0) {
            description += ", %d could not be read (see the log)".formatted(failed);
        }
        applicationEventPublisher.publishEvent(
                getBackgroundProcessEventBuilder()
                        .eventType(BackgroundProcessEventType.ENDED)
                        .description(description)
                        .build());
    }

    /**
     * At most every {@link #PROGRESS_INTERVAL_NANOS}, and always for the last file: thousands of UI updates per
     * scan would only flood the JavaFX thread (item 5.3).
     */
    private void publishProgressThrottled(int done, int total) {
        long now = System.nanoTime();
        if (done < total && now - lastProgressNanos < PROGRESS_INTERVAL_NANOS) {
            return;
        }
        lastProgressNanos = now;
        applicationEventPublisher.publishEvent(
                getBackgroundProcessEventBuilder()
                        .eventType(BackgroundProcessEventType.IN_PROGRESS)
                        .progress(done)
                        .maxProgress(total)
                        .description("Fetching metadata...")
                        .build());
    }

    private void publishInterruptedEvent() {
        applicationEventPublisher.publishEvent(
                getBackgroundProcessEventBuilder()
                        .eventType(BackgroundProcessEventType.INTERRUPTED)
                        .description("Interrupted")
                        .build());
    }

    private void publishInProgressEvent(int total) {
        applicationEventPublisher.publishEvent(
                getBackgroundProcessEventBuilder()
                        .eventType(BackgroundProcessEventType.IN_PROGRESS)
                        .maxProgress(total)
                        .description("Fetching metadata...")
                        .build());
    }

    private void publishStartedEvent() {
        applicationEventPublisher.publishEvent(
                getBackgroundProcessEventBuilder()
                        .eventType(BackgroundProcessEventType.STARTED)
                        .description("Discovering files...")
                        .maxProgress(-1)
                        .build());
    }

    private BackgroundProcessEvent.BackgroundProcessEventBuilder getBackgroundProcessEventBuilder() {
        return BackgroundProcessEvent
                .builder()
                .source(this)
                .processName(LIBRARY_SCAN);
    }

    // Uncomment if need to bulk fix lyrics again
//    @Override
//    public void onStartup() {
//        List<TrackRecord> trackRecords = dataAccess.getAllTracks();
//        for (TrackRecord trackRecord : trackRecords) {
//            autoFixLyrics(trackRecord);
//        }
//    }
}
