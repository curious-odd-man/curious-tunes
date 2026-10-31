package com.github.curiousoddman.curious_tunes.util;

import java.util.ArrayList;
import java.util.List;

/**
 * The folders that were scanned last, most recent first. They are stored in one preference value
 * (one folder per line), which has a limited length.
 */
public final class RecentFolders {
    private RecentFolders() {
    }

    public static List<String> parse(String stored) {
        List<String> result = new ArrayList<>();
        if (stored == null) {
            return result;
        }
        for (String line : stored.split("\\R")) {
            if (!line.isBlank()) {
                result.add(line.strip());
            }
        }
        return result;
    }

    public static String join(List<String> folders) {
        return String.join("\n", folders);
    }

    /**
     * Puts the folder first (a folder that is already in the list moves to the front), keeps at most
     * {@code maxItems} folders and drops the oldest ones until the stored text fits into {@code maxChars}.
     */
    public static List<String> push(List<String> current, String folder, int maxItems, int maxChars) {
        List<String> result = new ArrayList<>();
        if (folder != null && !folder.isBlank()) {
            result.add(folder.strip());
        }
        for (String existing : current) {
            if (!result.contains(existing) && result.size() < maxItems) {
                result.add(existing);
            }
        }
        while (result.size() > 1 && join(result).length() > maxChars) {
            result.removeLast();
        }
        return result;
    }
}
