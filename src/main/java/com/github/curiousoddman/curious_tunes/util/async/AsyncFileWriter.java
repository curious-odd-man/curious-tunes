package com.github.curiousoddman.curious_tunes.util.async;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.*;

public class AsyncFileWriter implements AutoCloseable {
    private final ExecutorService executor;
    private final BufferedWriter writer;

    /**
     * @param filePath path to the output file
     * @param append   true = append to existing file, false = overwrite
     */
    public AsyncFileWriter(String filePath, boolean append) throws IOException {
        Path path = Path.of(filePath);

        OpenOption[] options = append
                ? new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.APPEND}
                : new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE};

        this.writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, options);

        // Single thread = writes are always executed in submission order
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "async-file-writer");
            t.setDaemon(true);
            return t;
        });
    }

    public AsyncFileWriter(String filePath) throws IOException {
        this(filePath, false);
    }

    /**
     * Schedules a write. Returns a future you can await or chain.
     * Order is guaranteed to match the order of calls.
     */
    public CompletableFuture<Void> write(String content) {
        return CompletableFuture.runAsync(() -> {
            try {
                writer.write(content);
                writer.flush();
            } catch (IOException e) {
                throw new CompletionException("Write failed", e);
            }
        }, executor);
    }

    /**
     * Schedules a write of a line followed by a newline.
     */
    public CompletableFuture<Void> writeLine(String line) {
        return write(line + System.lineSeparator());
    }

    /**
     * Waits for all pending writes to finish, then closes the file.
     * Safe to call instead of (or in addition to) try-with-resources.
     */
    public void shutdown() throws InterruptedException, IOException {
        executor.shutdown();
        boolean finished = executor.awaitTermination(30, TimeUnit.SECONDS);
        if (!finished) {
            executor.shutdownNow();
        }
        writer.close();
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            boolean finished = executor.awaitTermination(30, TimeUnit.SECONDS);
            if (!finished) {
                executor.shutdownNow();
            }
            writer.close();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to close AsyncFileWriter", e);
        }
    }
}