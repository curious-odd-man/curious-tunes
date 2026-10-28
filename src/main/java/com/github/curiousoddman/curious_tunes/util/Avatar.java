package com.github.curiousoddman.curious_tunes.util;

import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Generated "profile picture" of an artist: initials on a colour that comes from the name,
 * so the same artist always looks the same.
 */
public final class Avatar {
    private Avatar() {
    }

    /**
     * Up to two letters: the first letters of the first two words ("Pink Floyd" gives "PF"), or the first
     * two letters of a single word ("Queen" gives "QU"). Anything that is neither a letter nor a digit is ignored.
     */
    public static String initials(String name) {
        if (name == null) {
            return "?";
        }
        List<String> words = new ArrayList<>();
        for (String word : name.trim()
                               .split("\\s+")) {
            String alphanumeric = word.replaceAll("[^\\p{L}\\p{N}]", "");
            if (!alphanumeric.isEmpty()) {
                words.add(alphanumeric);
            }
        }
        if (words.isEmpty()) {
            return "?";
        }
        if (words.size() == 1) {
            String word = words.getFirst();
            return word.substring(0, Math.min(2, word.length()))
                       .toUpperCase();
        }
        return (words.get(0)
                     .substring(0, 1) + words.get(1)
                                             .substring(0, 1)).toUpperCase();
    }

    /**
     * A calm colour that is dark enough for white text (contrast of about 4.5 or better).
     */
    public static Color color(String name) {
        int    hash = name == null ? 0 : name.toLowerCase()
                                             .hashCode();
        double hue  = Math.floorMod(hash, 360);
        return Color.hsb(hue, 0.55, 0.52);
    }
}
