package com.github.curiousoddman.curious_tunes.domain.user.prefs;

import com.github.curiousoddman.curious_tunes.domain.DataAccess;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserPreferencesService {
    public static final double DEFAULT_WINDOW_X = 100;
    public static final double DEFAULT_WINDOW_Y = 100;
    public static final double DEFAULT_WINDOW_WIDTH = 1920;
    public static final double DEFAULT_WINDOW_HEIGHT = 1080;
    public static final int DEFAULT_VOLUME = 30;
    public static final double DEFAULT_ARTISTS_SPLIT_WIDTH = 0.26;
    public static final double DEFAULT_PLAYLIST_SPLIT_WIDTH = 0.8;

    private final DataAccess dataAccess;

    public void saveWindowState(Stage stage) {
        if (!stage.isMaximized()) {
            dataAccess.setUserPref(UserPrefKey.WINDOW_X, String.valueOf(stage.getX()));
            dataAccess.setUserPref(UserPrefKey.WINDOW_Y, String.valueOf(stage.getY()));
            dataAccess.setUserPref(UserPrefKey.WINDOW_WIDTH, String.valueOf(stage.getWidth()));
            dataAccess.setUserPref(UserPrefKey.WINDOW_HEIGHT, String.valueOf(stage.getHeight()));
        }
        dataAccess.setUserPref(UserPrefKey.WINDOW_MAXIMIZED, String.valueOf(stage.isMaximized()));
        log.info("Window state saved (maximized={})", stage.isMaximized());
    }

    public void restoreWindowState(Stage stage) {
        stage.setX(getDouble(UserPrefKey.WINDOW_X, DEFAULT_WINDOW_X));
        stage.setY(getDouble(UserPrefKey.WINDOW_Y, DEFAULT_WINDOW_Y));
        stage.setWidth(getDouble(UserPrefKey.WINDOW_WIDTH, DEFAULT_WINDOW_WIDTH));
        stage.setHeight(getDouble(UserPrefKey.WINDOW_HEIGHT, DEFAULT_WINDOW_HEIGHT));
        stage.setMaximized(getBoolean(UserPrefKey.WINDOW_MAXIMIZED, false));
        log.info("Window state restored");
    }

    public void saveVolume(int volume) {
        dataAccess.setUserPref(UserPrefKey.VOLUME, String.valueOf(Math.max(0, Math.min(100, volume))));
    }

    private double getDouble(UserPrefKey key, double defaultValue) {
        String raw = dataAccess.getUserPref(key, null);
        if (raw == null) return defaultValue;
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            log.warn("Corrupt pref [{}]='{}', using default {}", key.getKey(), raw, defaultValue);
            return defaultValue;
        }
    }

    private int getInt(UserPrefKey key, int defaultValue) {
        return (int) Math.round(getDouble(key, defaultValue));
    }

    private boolean getBoolean(UserPrefKey key, boolean defaultValue) {
        String raw = dataAccess.getUserPref(key, null);
        if (raw == null) return defaultValue;
        return Boolean.parseBoolean(raw);
    }

    public int getVolume() {
        return getInt(UserPrefKey.VOLUME, DEFAULT_VOLUME);
    }

    public double[] getDividerPositions() {
        return new double[]{
                getDouble(UserPrefKey.ARTISTS_SPLIT_WIDTH, DEFAULT_ARTISTS_SPLIT_WIDTH),
                getDouble(UserPrefKey.PLAYLIST_SPLIT_WIDTH, DEFAULT_PLAYLIST_SPLIT_WIDTH)
        };
    }

    public void saveSplitPositions(double[] dividerPositions) {
        dataAccess.setUserPref(UserPrefKey.ARTISTS_SPLIT_WIDTH, String.valueOf(dividerPositions[0]));
        dataAccess.setUserPref(UserPrefKey.PLAYLIST_SPLIT_WIDTH, String.valueOf(dividerPositions[1]));
    }
}