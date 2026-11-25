package com.github.curiousoddman.curious_tunes.domain.audio;

import com.sun.media.jfxmedia.locator.StreamSources;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Fails when JavaFX is upgraded without re-checking the media patch (src/jfxPatch): the patched {@code Locator} is
 * a copy of one JavaFX version's file. To upgrade: diff the new JavaFX {@code Locator.java} and
 * {@code ConnectionHolder.java} against {@code src/jfxPatch/original/}, re-apply the two "cTunes patch" edits,
 * update both version constants, store the new originals, and play an ALAC track (start, seek, next track).
 * See doc/performance-review.md, section 1, step 8.
 */
class JfxPatchVersionTest {

    @Test
    void patchWasMadeForTheJavafxVersionInUse() throws Exception {
        Class<?> versionInfo = Class.forName("com.sun.javafx.runtime.VersionInfo");
        String version = (String) versionInfo.getMethod("getVersion")
                                             .invoke(null);
        assertEquals(StreamSources.PATCH_VERSION, version,
                "JavaFX is " + version + " but the media patch was made for " + StreamSources.PATCH_VERSION);
    }
}
