package com.github.curiousoddman.curious_tunes.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/**
 * How the History tab shows times: "Today 14:03", "Yesterday 21:15", "12 Oct 2026", "42 h 15 min".
 */
public final class HistoryFormat {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private HistoryFormat() {
    }

    /**
     * @param now the current moment (a parameter so that this can be tested)
     */
    public static String playedAt(OffsetDateTime playedAt, Instant now, ZoneId zone, Locale locale) {
        ZonedDateTime time  = playedAt.atZoneSameInstant(zone);
        LocalDate     today = now.atZone(zone).toLocalDate();
        LocalDate     day   = time.toLocalDate();
        if (day.equals(today)) {
            return "Today " + time.format(TIME);
        }
        if (day.equals(today.minusDays(1))) {
            return "Yesterday " + time.format(TIME);
        }
        return day.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale));
    }

    /**
     * The complete date and time, for a tooltip.
     */
    public static String full(OffsetDateTime playedAt, ZoneId zone, Locale locale) {
        return playedAt.atZoneSameInstant(zone)
                       .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale));
    }

    /**
     * A listening time in the two largest units.
     */
    public static String duration(long totalSeconds) {
        long seconds = Math.max(0, totalSeconds);
        long days    = seconds / 86_400;
        long hours   = (seconds % 86_400) / 3_600;
        long minutes = (seconds % 3_600) / 60;
        if (days > 0) {
            return days + " d " + hours + " h";
        }
        if (hours > 0) {
            return hours + " h " + minutes + " min";
        }
        if (minutes > 0) {
            return minutes + " min";
        }
        return seconds + " s";
    }
}
