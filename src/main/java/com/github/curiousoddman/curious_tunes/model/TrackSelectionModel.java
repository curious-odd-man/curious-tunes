package com.github.curiousoddman.curious_tunes.model;

import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Selection of the tracks shown for one artist: single, Ctrl-toggle and Shift-range (within one album).
 * Tracks register themselves in display order while the album views are being built.
 */
public class TrackSelectionModel {
    private final List<TrackInfo>           registered = Collections.synchronizedList(new ArrayList<>());
    private final ObservableList<TrackInfo> selected   = FXCollections.observableArrayList();
    private       TrackInfo                 anchor;
    private final Map<Integer, Node>        rows       = new ConcurrentHashMap<>();

    public void register(TrackInfo trackInfo) {
        registered.add(trackInfo);
    }

    /**
     * Registers a track together with the row that shows it (used to scroll to the row).
     */
    public void register(TrackInfo trackInfo, Node row) {
        registered.add(trackInfo);
        if (trackInfo.getTrackId() != null) {
            rows.put(trackInfo.getTrackId(), row);
        }
    }

    public Node rowOf(TrackInfo trackInfo) {
        return trackInfo.getTrackId() == null ? null : rows.get(trackInfo.getTrackId());
    }

    public TrackInfo getAnchor() {
        return anchor;
    }

    /**
     * All tracks in the order they are shown: album by album (in the given order of albums), each album in its
     * own track order.
     */
    public List<TrackInfo> inDisplayOrder(List<Integer> albumIds) {
        List<TrackInfo> result = new ArrayList<>();
        synchronized (registered) {
            for (Integer albumId : albumIds) {
                for (TrackInfo track : registered) {
                    if (Objects.equals(track.getAlbumId(), albumId)) {
                        result.add(track);
                    }
                }
            }
        }
        return result;
    }

    /**
     * @return the registered track with this id, or null
     */
    public TrackInfo findByTrackId(Integer trackId) {
        synchronized (registered) {
            for (TrackInfo track : registered) {
                if (Objects.equals(track.getTrackId(), trackId)) {
                    return track;
                }
            }
        }
        return null;
    }

    public ObservableList<TrackInfo> getSelectedItems() {
        return FXCollections.unmodifiableObservableList(selected);
    }

    public boolean isSelected(TrackInfo trackInfo) {
        return selected.stream()
                       .anyMatch(t -> Objects.equals(t.getTrackId(), trackInfo.getTrackId()));
    }

    public void selectOnly(TrackInfo trackInfo) {
        anchor = trackInfo;
        selected.setAll(trackInfo);
    }

    public void toggle(TrackInfo trackInfo) {
        anchor = trackInfo;
        if (!selected.removeIf(t -> Objects.equals(t.getTrackId(), trackInfo.getTrackId()))) {
            selected.add(trackInfo);
        }
    }

    /**
     * Selects everything between the anchor and the given track. Ranges are limited to one album;
     * for a track of another album only that track is selected.
     */
    public void selectRange(TrackInfo trackInfo) {
        if (anchor == null || !Objects.equals(anchor.getAlbumId(), trackInfo.getAlbumId())) {
            selectOnly(trackInfo);
            return;
        }
        List<TrackInfo> sameAlbum;
        synchronized (registered) {
            sameAlbum = registered.stream()
                                  .filter(t -> Objects.equals(t.getAlbumId(), trackInfo.getAlbumId()))
                                  .toList();
        }
        int from = indexOf(sameAlbum, anchor);
        int to   = indexOf(sameAlbum, trackInfo);
        if (from < 0 || to < 0) {
            selectOnly(trackInfo);
            return;
        }
        selected.setAll(sameAlbum.subList(Math.min(from, to), Math.max(from, to) + 1));
    }

    /**
     * @return selected tracks in the order they are displayed
     */
    public List<TrackInfo> getSelectedInOrder() {
        List<TrackInfo> result = new ArrayList<>();
        synchronized (registered) {
            for (TrackInfo track : registered) {
                if (isSelected(track)) {
                    result.add(track);
                }
            }
        }
        return result;
    }

    private static int indexOf(List<TrackInfo> tracks, TrackInfo track) {
        for (int i = 0; i < tracks.size(); i++) {
            if (Objects.equals(tracks.get(i)
                                     .getTrackId(), track.getTrackId())) {
                return i;
            }
        }
        return -1;
    }
}
