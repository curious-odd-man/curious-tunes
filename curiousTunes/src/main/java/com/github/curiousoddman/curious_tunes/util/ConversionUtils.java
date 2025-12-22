package com.github.curiousoddman.curious_tunes.util;

import lombok.experimental.UtilityClass;

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
}
