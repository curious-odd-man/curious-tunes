package com.github.curiousoddman.curious_tunes.domain.tags;

import com.github.curiousoddman.curious_tunes.domain.AudioFormat;
import org.mp4parser.CustomizedPropertyBoxParser;
import com.mpatric.mp3agic.Mp3File;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.mp4parser.Box;
import org.mp4parser.IsoFile;
import org.mp4parser.boxes.UnknownBox;
import org.mp4parser.boxes.iso14496.part12.FreeBox;
import org.mp4parser.support.AbstractBox;
import org.mp4parser.support.AbstractContainerBox;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@Slf4j
@Component
public class MetadataManager {

    public MetadataTags getMetadata(Path file) {
        try {
            AudioFormat format = AudioFormat.of(file)
                                             .orElseThrow(() -> new IllegalArgumentException("Unsupported file format: " + file));
            return switch (format) {
                case M4A -> extractM4aMetadata(file);
                case MP3 -> extractMp3Tags(file);
            };
        } catch (Exception e) {
            log.error("Failed to parse file {}", file, e);
            throw e;
        }
    }

    @SneakyThrows
    private MetadataTags extractMp3Tags(Path file) {
        Mp3File mp3file = new Mp3File(file);
        return new Mp3MetadataTags(mp3file, file);
    }

    @SneakyThrows
    private static MetadataTags extractM4aMetadata(Path file) {
        try (FileInputStream fileInputStream = new FileInputStream(file.toFile())) {
            List<Box> resultBoxes = new ArrayList<>();

            IsoFile isoFile = new IsoFile(
                    fileInputStream.getChannel(),
                    new CustomizedPropertyBoxParser()
            );
            Queue<Box> boxesQueue = new LinkedList<>(isoFile.getBoxes());
            while (!boxesQueue.isEmpty()) {
                Box box = boxesQueue.remove();
                if (box instanceof AbstractContainerBox container) {
                    boxesQueue.addAll(container.getBoxes());
                }
                if (box instanceof AbstractBox abstractBox) {
                    abstractBox.parseDetails();
                }
                resultBoxes.add(box);
            }

            List<Box> allBoxes = resultBoxes
                    .stream()
                    .filter(rb -> !(rb instanceof FreeBox))
                    .filter(rb -> !(rb instanceof UnknownBox))
                    .toList();
            return new Mp4MetadataTags(isoFile, allBoxes, file);
        } catch (Exception e) {
            log.error("Failed to extract tags {}", file, e);
            return new Mp4FailedParsingTags(file.toString(), e.getMessage());
        }
    }
}
