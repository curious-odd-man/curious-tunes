package com.github.curiousoddman.curious_tunes.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Shows a file in the file manager of the operating system.
 */
@Slf4j
public final class FileManager {
    private FileManager() {
    }

    /**
     * Opens the folder of the file and, where the system supports it, selects the file.
     *
     * @return false when the file manager could not be started
     */
    public static boolean reveal(String location) {
        if (location == null || location.isBlank()) {
            return false;
        }
        Path   file = Path.of(location);
        String os   = System.getProperty("os.name", "")
                            .toLowerCase(Locale.ROOT);
        try {
            if (os.contains("win")) {
                new ProcessBuilder("explorer.exe", "/select,", file.toString()).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", "-R", file.toString()).start();
            } else {
                Path folder = file.getParent() == null ? file : file.getParent();
                new ProcessBuilder("xdg-open", folder.toString()).start();
            }
            return true;
        } catch (IOException e) {
            log.warn("Unable to open the file manager for {}", location, e);
            return false;
        }
    }
}
