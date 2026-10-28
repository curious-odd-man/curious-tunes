package com.github.curiousoddman.curious_tunes.domain.user.prefs;

import com.github.curiousoddman.curious_tunes.domain.DataAccess;
import com.github.curiousoddman.curious_tunes.model.AlbumSort;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserPreferencesService {
    public static final  double DEFAULT_WINDOW_X               = 100;
    public static final  double DEFAULT_WINDOW_Y               = 100;
    public static final  double DEFAULT_WINDOW_WIDTH           = 1920;
    public static final  double DEFAULT_WINDOW_HEIGHT          = 1080;
    public static final  int    DEFAULT_VOLUME                 = 30;
    public static final  double DEFAULT_ARTISTS_SPLIT_WIDTH    = 0.26;
    public static final  double DEFAULT_PLAYLIST_SPLIT_WIDTH   = 0.8;
    public static final  double DEFAULT_LYRICS_FONT_SIZE       = 12;
    public static final  double DEFAULT_PLAYBACK_WINDOW_WIDTH  = 800;
    public static final  double DEFAULT_PLAYBACK_WINDOW_HEIGHT = 340;
    public static final  double DEFAULT_ALBUMS_LIST_COVER_SIZE = 350;
    public static final  double DEFAULT_ALBUMS_GRID_COVER_SIZE = 180;
    private static final int    MAX_PREF_VALUE_LENGTH          = 255;     // size of user_preferences.pref_value

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

    /**
     * Saves position and size of the playback window in its simple (not maximized) view.
     */
    public void savePlaybackWindowBounds(double x, double y, double width, double height) {
        dataAccess.setUserPref(UserPrefKey.PLAYBACK_WINDOW_X, String.valueOf(x));
        dataAccess.setUserPref(UserPrefKey.PLAYBACK_WINDOW_Y, String.valueOf(y));
        dataAccess.setUserPref(UserPrefKey.PLAYBACK_WINDOW_WIDTH, String.valueOf(width));
        dataAccess.setUserPref(UserPrefKey.PLAYBACK_WINDOW_HEIGHT, String.valueOf(height));
    }

    /**
     * @return {x, y, width, height} of the playback window, or null if it was never saved
     */
    public double[] getPlaybackWindowBounds() {
        double x      = getDouble(UserPrefKey.PLAYBACK_WINDOW_X, Double.NaN);
        double y      = getDouble(UserPrefKey.PLAYBACK_WINDOW_Y, Double.NaN);
        double width  = getDouble(UserPrefKey.PLAYBACK_WINDOW_WIDTH, Double.NaN);
        double height = getDouble(UserPrefKey.PLAYBACK_WINDOW_HEIGHT, Double.NaN);
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(width) || Double.isNaN(height)) {
            return null;
        }
        return new double[]{x, y, width, height};
    }

    public void saveVolume(int volume) {
        dataAccess.setUserPref(UserPrefKey.VOLUME, String.valueOf(Math.max(0, Math.min(100, volume))));
    }

    private double getDouble(UserPrefKey key, double defaultValue) {
        String raw = dataAccess.getUserPref(key, null);
        if (raw == null) {
            return defaultValue;
        }
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
        if (raw == null) {
            return defaultValue;
        }
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

    public boolean isPlaybackLyricsOpen() {
        return getBoolean(UserPrefKey.PLAYBACK_LYRICS_OPEN, false);
    }

    public void savePlaybackLyricsOpen(boolean open) {
        dataAccess.setUserPref(UserPrefKey.PLAYBACK_LYRICS_OPEN, String.valueOf(open));
    }

    public boolean isPlaybackLyricsAutoFit() {
        return getBoolean(UserPrefKey.PLAYBACK_LYRICS_AUTOFIT, true);
    }

    public void savePlaybackLyricsAutoFit(boolean autoFit) {
        dataAccess.setUserPref(UserPrefKey.PLAYBACK_LYRICS_AUTOFIT, String.valueOf(autoFit));
    }

    public double getPlaybackLyricsSize() {
        return getDouble(UserPrefKey.PLAYBACK_LYRICS_SIZE, DEFAULT_LYRICS_FONT_SIZE);
    }

    public void savePlaybackLyricsSize(double size) {
        dataAccess.setUserPref(UserPrefKey.PLAYBACK_LYRICS_SIZE, String.valueOf(size));
    }

    public void saveLyricsFontSize(double size) {
        dataAccess.setUserPref(UserPrefKey.LYRICS_FONT_SIZE, String.valueOf(size));
    }

    public double getLyricsFontSize() {
        return getDouble(UserPrefKey.LYRICS_FONT_SIZE, DEFAULT_LYRICS_FONT_SIZE);
    }

    public AlbumSort getAlbumSort() {
        return AlbumSort.fromName(dataAccess.getUserPref(UserPrefKey.ALBUMS_SORT, null));
    }

    public void saveAlbumSort(AlbumSort sort) {
        dataAccess.setUserPref(UserPrefKey.ALBUMS_SORT, sort.name());
    }

    public boolean isAlbumsGridView() {
        return getBoolean(UserPrefKey.ALBUMS_GRID_VIEW, false);
    }

    public void saveAlbumsGridView(boolean grid) {
        dataAccess.setUserPref(UserPrefKey.ALBUMS_GRID_VIEW, String.valueOf(grid));
    }

    public double getAlbumsCoverSize(boolean grid) {
        return grid
                ? getDouble(UserPrefKey.ALBUMS_GRID_COVER_SIZE, DEFAULT_ALBUMS_GRID_COVER_SIZE)
                : getDouble(UserPrefKey.ALBUMS_LIST_COVER_SIZE, DEFAULT_ALBUMS_LIST_COVER_SIZE);
    }

    public void saveAlbumsCoverSize(boolean grid, double size) {
        dataAccess.setUserPref(grid ? UserPrefKey.ALBUMS_GRID_COVER_SIZE : UserPrefKey.ALBUMS_LIST_COVER_SIZE,
                String.valueOf(size));
    }

    /**
     * @return the folder used for the last library scan, or the user's "Music" folder if there was none
     */
    public String getLibraryRoot() {
        String saved = dataAccess.getUserPref(UserPrefKey.LIBRARY_ROOT, null);
        if (saved != null && !saved.isBlank()) {
            return saved;
        }
        return Path.of(System.getProperty("user.home"), "Music")
                   .toString();
    }

    public void saveLibraryRoot(String libraryRoot) {
        if (libraryRoot == null || libraryRoot.isBlank() || libraryRoot.length() > MAX_PREF_VALUE_LENGTH) {
            log.warn("Library root is not saved (empty or longer than {} characters)", MAX_PREF_VALUE_LENGTH);
            return;
        }
        dataAccess.setUserPref(UserPrefKey.LIBRARY_ROOT, libraryRoot);
    }
}