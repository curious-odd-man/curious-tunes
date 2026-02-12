package com.github.curiousoddman.curious_tunes.util;

import org.jooq.Field;
import org.jooq.TableField;
import org.jooq.impl.UpdatableRecordImpl;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static com.github.curiousoddman.curious_tunes.dbobj.tables.Track.TRACK;

public class JooqUtils {
    public static <T, V extends UpdatableRecordImpl<V>> void updateFieldIfChanged(V trackRecord, T value, TableField<V, T> field) {
        if (!Objects.equals(value, trackRecord.get(TRACK.FILE_LOCATION))) {
            trackRecord.set(field, value);
        }
    }

    public static <T> void updateIfChanged(T oldValue, T newValue, Consumer<T> setter, List<Field<?>> fieldsToUpdate, Field<?> field) {
        if (!Objects.equals(oldValue, newValue)) {
            setter.accept(newValue);
            fieldsToUpdate.add(field);
        }
    }
}
