package com.github.curiousoddman.curious_tunes.util;

import lombok.experimental.UtilityClass;

import java.util.function.Consumer;
import java.util.function.Function;

@UtilityClass
public class ConversionUtils {

    public static Integer asInteger(Object o) {
        if (o == null) {
            return null;
        } else if (o instanceof Integer i) {
            return i;
        } else {
            return Integer.parseInt(o.toString());
        }
    }

    public static <T> void setIfDefined(Consumer<T> setter, String value, Function<String, T> mapper) {
        if (value == null) {
            return;
        }
        if (value.isBlank()) {
            return;
        }
        setter.accept(mapper.apply(value));
    }

    public static Integer toInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null; // or handle validation error
        }
    }

    public static String str(Object value) {
        return value == null ? "" : value.toString();
    }
}
