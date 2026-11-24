package com.github.curiousoddman.curious_tunes.domain;

/**
 * A file's size and last-modified time (milliseconds since 1970-01-01 UTC), as stored at its last scan. The library
 * scan doesn't read a file again while both are unchanged (doc/performance-review.md, item 5.1). Either may be null
 * for tracks scanned before these were stored.
 */
public record FileStamp(Long size, Long modifiedMillis) {

    /**
     * @return true when the stored stamp is complete and equal to the file's current size and time
     */
    public boolean matches(long currentSize, long currentModifiedMillis) {
        return size != null && modifiedMillis != null && size == currentSize && modifiedMillis == currentModifiedMillis;
    }
}
