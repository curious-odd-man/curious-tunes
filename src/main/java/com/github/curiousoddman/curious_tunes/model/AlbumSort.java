package com.github.curiousoddman.curious_tunes.model;

/**
 * How the albums of an artist are ordered.
 */
public enum AlbumSort {
    YEAR_ASC("Year, oldest first"),
    YEAR_DESC("Year, newest first"),
    NAME("Name");

    private final String label;

    AlbumSort(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static AlbumSort fromName(String name) {
        for (AlbumSort sort : values()) {
            if (sort.name()
                    .equals(name)) {
                return sort;
            }
        }
        return YEAR_ASC;
    }
}
