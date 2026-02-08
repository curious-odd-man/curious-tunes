package com.github.curiousoddman.curious_tunes.model;

import com.github.curiousoddman.curious_tunes.backend.DataAccess;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.event.AddToPlaylistEvent;
import com.github.curiousoddman.curious_tunes.event.player.PlayerStatusEvent;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.swap;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaylistModel {
    @Getter
    private final List<PlaylistItem> playlistItems = new ArrayList<>();
    private final PlaylistSelectionModel selectionModel = new PlaylistSelectionModel(playlistItems);
    private final PlaylistSelectionModel currentlyPlayingModel = new PlaylistSelectionModel(playlistItems);

    private final DataAccess dataAccess;

    public void addItems(AddToPlaylistEvent addToPlaylistEvent) {
        PlaylistItem selectedItem = currentlyPlayingModel.getSelectedItem();
        List<PlaylistItem> tracksToAdd = new ArrayList<>();
        if (addToPlaylistEvent.getTracks() != null) {
            List<PlaylistItem> list = addToPlaylistEvent
                    .getTracks()
                    .stream()
                    .map(tqi -> new PlaylistItem(tqi.trackRecord(), tqi.trackArtist(), tqi.trackAlbum()))
                    .toList();
            tracksToAdd.addAll(list);
        }
        if (addToPlaylistEvent.getArtistRecord() != null) {
            List<AlbumTrackItem> artistTracks = dataAccess.getArtistTracks(addToPlaylistEvent.getArtistRecord());
            List<PlaylistItem> list = artistTracks
                    .stream()
                    .map(at -> new PlaylistItem(at.trackRecord(), addToPlaylistEvent.getArtistRecord(), at.trackAlbum()))
                    .toList();
            tracksToAdd.addAll(list);
        }
        if (addToPlaylistEvent.getAlbums() != null) {
            List<AlbumTrackItem> albumsTracks = dataAccess.getAlbumsTracks(addToPlaylistEvent.getAlbums());
            Set<Integer> artistFks = albumsTracks
                    .stream()
                    .map(AlbumTrackItem::trackAlbum)
                    .map(AlbumRecord::getFkArtist)
                    .collect(Collectors.toSet());
            Map<Integer, ArtistRecord> artistRecordMap = dataAccess
                    .getArtists(artistFks)
                    .stream()
                    .collect(Collectors.toMap(ArtistRecord::getId, Function.identity()));
            List<PlaylistItem> list = albumsTracks
                    .stream()
                    .map(at -> new PlaylistItem(at.trackRecord(), artistRecordMap.get(at.trackAlbum().getFkArtist()), at.trackAlbum()))
                    .toList();
            tracksToAdd.addAll(list);
        }
        boolean replacePlaylist = addToPlaylistEvent.getPlaylistAddMode() == PlaylistAddMode.REPLACE;
        if (replacePlaylist) {
            clear();
        }
        Shuffle shuffle = addToPlaylistEvent.getShuffle();
        if (shuffle == Shuffle.SKIP) {
            playlistItems.addAll(tracksToAdd);
        } else if (shuffle == Shuffle.BEFORE_ADDING_TO_PLAYLIST) {
            Collections.shuffle(tracksToAdd);
            playlistItems.addAll(tracksToAdd);
        } else if (shuffle == Shuffle.AFTER_ADDING_TO_PLAYLIST) {
            playlistItems.addAll(tracksToAdd);
            Collections.shuffle(playlistItems);
        } else {
            log.error("Unknown type of shuffle {}", shuffle);
            playlistItems.addAll(tracksToAdd);
        }
        if (selectedItem != null) {
            if (!replacePlaylist) {
                playlistItems.remove(selectedItem);
            }
            playlistItems.addFirst(selectedItem);
            selectionModel.select(0);
            currentlyPlayingModel.select(0);
        }
        selectionModel.clearSelection();
    }

    public void clear() {
        selectionModel.clearSelection();
        currentlyPlayingModel.clearSelection();
        playlistItems.clear();
    }

    public OptionalInt moveSelectedUp() {
        Optional<PlaylistItem> current = currentlyPlayingModel.getOptionalSelectedItem();
        current.ifPresent(playlistItems::remove);

        int selectedIndex = selectionModel.getSelectedIndex();
        if (selectedIndex <= 0) {
            return OptionalInt.empty();
        }

        int newSelectedIndex = selectedIndex - 1;
        swap(playlistItems, selectedIndex, newSelectedIndex);
        selectionModel.select(newSelectedIndex);
        current.ifPresent(playlistItems::addFirst);
        logPlaylistItems();
        return OptionalInt.of(selectedIndex);
    }

    public OptionalInt moveSelectedDown() {
        PlaylistItem current = currentlyPlayingModel.getSelectedItem();

        int selectedIndex = selectionModel.getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= playlistItems.size()) {
            return OptionalInt.empty();
        }

        int newSelectedIndex = selectedIndex + 1;
        swap(playlistItems, selectedIndex, newSelectedIndex);
        selectionModel.select(newSelectedIndex);
        currentlyPlayingModel.select(current);
        logPlaylistItems();
        return OptionalInt.of(selectedIndex);
    }

    public PlaylistItem getSelected() {
        return selectionModel.getSelectedItem();
    }

    public OptionalInt deleteSelected() {
        int selectedIndex = selectionModel.getSelectedIndex();
        if (selectedIndex < 0) {
            return OptionalInt.empty();
        }
        playlistItems.remove(selectedIndex);
        selectionModel.clearSelection();
        logPlaylistItems();
        return OptionalInt.of(selectedIndex);
    }

    public void shuffle() {
        Optional<PlaylistItem> current = currentlyPlayingModel.getOptionalSelectedItem();
        current.ifPresent(playlistItems::remove);
        Collections.shuffle(playlistItems);
        selectionModel.clearSelection();
        current.ifPresent(playlistItems::addFirst);
        current.ifPresent(currentlyPlayingModel::select);
        logPlaylistItems();
    }

    public Optional<PlaylistItem> getNextForPlayback() {
        logPlaylistItems();
        int nextIndex = currentlyPlayingModel.getSelectedIndex() + 1;
        if (nextIndex < playlistItems.size()) {
            PlaylistItem playlistItem = playlistItems.get(nextIndex);
            log.info("Next item to play is: {}", playlistItem.getTrackRecord().getTitle());
            return Optional.of(playlistItem);
        }
        return Optional.empty();
    }

    private void logPlaylistItems() {
        MDC.put(".", "PLAYLIST");
        log.debug("Dumping playlist -------------------- {} items", playlistItems.size());
        int selectedIndex = selectionModel.getSelectedIndex();
        int playingIndex = currentlyPlayingModel.getSelectedIndex();
        for (int i = 0; i < playlistItems.size(); i++) {
            PlaylistItem playlistItem = playlistItems.get(i);
            log.debug(
                    "\t {}{} {} {}",
                    selectedIndex == i ? "S" : " ",
                    playingIndex == i ? "P" : " ",
                    playlistItem.getPlaylistItemStatus(),
                    playlistItem.getTrackRecord().getTitle()
            );
        }
        log.info("END");
        MDC.remove(".");
    }

    public ReadOnlyObjectProperty<PlaylistItem> getSelectedProperty() {
        return selectionModel.selectedItemProperty();
    }

    public void select(PlaylistItem playlistItem) {
        selectionModel.select(playlistItem);
    }

    public void updateStatuses(PlayerStatusEvent playerStatusEvent) {
        PlaylistItem playlistItem = playerStatusEvent.getPlaylistItem();
        switch (playerStatusEvent.getStatus()) {
            case NONE, PAUSED, PLAYING -> {
            }
            case LAUNCHING -> {
                playlistItem.setPlaylistItemStatus(PlaylistItemStatus.PLAYING);
                currentlyPlayingModel.select(playlistItem);
            }
            case ENDED -> playlistItem.setPlaylistItemStatus(PlaylistItemStatus.PLAYED);
        }
    }

    public Optional<PlaylistItem> getCurrentlyPlaying() {
        return currentlyPlayingModel.getOptionalSelectedItem();
    }
}
