package com.github.curiousoddman.curious_tunes.util;

import org.jooq.TableField;
import org.jooq.impl.UpdatableRecordImpl;

import java.util.Objects;

import static com.github.curiousoddman.curious_tunes.dbobj.tables.Track.TRACK;

public class JooqUtils {
    public static <T, V extends UpdatableRecordImpl<V>> void updateFieldIfChanged(V trackRecord, T value, TableField<V, T> field) {
        if (!Objects.equals(value, trackRecord.get(TRACK.FILE_LOCATION))) {
            trackRecord.set(field, value);
        }
    }
}
