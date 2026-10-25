package com.github.curiousoddman.curious_tunes.model;

import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Selection of the tracks shown for one artist: single, Ctrl-toggle and Shift-range (within one album).
 * Tracks register themselves in display order while the album views are being built.
 */
public class TrackSelectionModel {
    private final List<TrackInfo>           registered = Collections.synchronizedList(new ArrayList<>());
    private final ObservableList<TrackInfo> selected   = FXCollections.observableArrayList();
    private       TrackInfo                 anchor;

    public void register(TrackInfo trackInfo) {
        registered.add(trackInfo);
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
