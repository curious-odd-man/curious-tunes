package com.github.curiousoddman.curious_tunes;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.mp4parser.Box;
import org.mp4parser.IsoFile;
import org.mp4parser.boxes.UnknownBox;
import org.mp4parser.boxes.apple.AppleCoverBox;
import org.mp4parser.boxes.apple.AppleLosslessSpecificBox;
import org.mp4parser.boxes.apple.AppleTrackNumberBox;
import org.mp4parser.boxes.iso14496.part12.FreeBox;
import org.mp4parser.support.AbstractBox;
import org.mp4parser.support.AbstractContainerBox;

import java.io.FileInputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

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
                if (!attrs.isRegularFile()) {
                    return FileVisitResult.CONTINUE;
                }
                System.out.print('.');
                if (i++ % 150 == 0) {
                    System.out.println();
                }
                try {
                    Map<String, String> tags = extractMp4ParserMetadata(file);
                    if (tags.isEmpty()) {
                        log.warn("{} contained no metadata", file);
                        return FileVisitResult.CONTINUE;
                    }
                    StringBuilder sb = new StringBuilder("File: ").append(file.toAbsolutePath()).append('\n');
                    tags.forEach((k, v) -> {
                        stats.compute(k, (_, vv) -> vv == null ? 1 : vv + 1);
                        sb.append('\t').append(k).append('\t').append(v).append('\n');
                    });
                    Files.writeString(resultPath, sb.toString(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                } catch (Exception e) {
                    log.error("{}: {}", file, e.getMessage(), e);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        StringBuilder sb = new StringBuilder();
        stats.forEach((k, v) ->
                sb.append(k).append('\t').append(v).append('\n')
        );
        Files.writeString(Path.of("tag-stats.txt"), sb.toString(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }


    @SneakyThrows
    private static Map<String, String> extractMp4ParserMetadata(Path file) {
        // https://github.com/sannies/mp4parser/blob/master/examples/src/main/java/org/mp4parser/examples/metadata/MetaDataRead.java
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
        Set<Class<? extends AbstractBox>> classes = Set.of(
                AppleLosslessSpecificBox.class,
                AppleTrackNumberBox.class,
                AppleCoverBox.class
        );

        resultBoxes
                .stream()
                .filter(rb -> !(rb instanceof FreeBox))
                .filter(rb -> !(rb instanceof UnknownBox))
                .forEach(rb -> {
                    if (classes.contains(rb.getClass())) {
                        System.out.println("debug");
                    }
                });
        return Map.of();
//                .collect(Collectors.toMap(
//                        rb -> switch (rb) {
//                            case HandlerBox handlerBox -> rb.getClass().getName() + "[" + handlerBox.getHandlerType() + "]";
//                            default -> rb.getClass().getName();
//                        },
//                        rb -> switch (rb) {
//                            case Utf8AppleDataBox utf8AppleDataBox -> utf8AppleDataBox.getValue();
//                            case AppleLosslessSpecificBox appleLosslessSpecificBox -> {
//                                System.out.println("Debug");
//                                yield  rb.toString();
//                            }
//                            case AppleTrackNumberBox appleTrackNumberBox -> {
//                               System.out.println("Debug");
//                               yield  rb.toString();
//                           }
//                            default -> rb.toString();
//                        }
//                ));

    }


}
