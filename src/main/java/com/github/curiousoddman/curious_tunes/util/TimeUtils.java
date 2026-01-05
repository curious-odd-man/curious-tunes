package com.github.curiousoddman.curious_tunes.util;

public class TimeUtils {
    public static String secondsToHumanTime(long seconds) {
        return secondsToHumanTime((int) seconds);
    }

    public static String secondsToHumanTime(int seconds) {
        int h = seconds / 3600;
        int remainder = seconds - h * 3600;
        int m = remainder / 60;
        int s = remainder - m * 60;

        if (h == 0) {
            return "%02d:%02d".formatted(m, s);
        }

        return "%02d:%02d:%02d".formatted(h, m, s);
    }

}
