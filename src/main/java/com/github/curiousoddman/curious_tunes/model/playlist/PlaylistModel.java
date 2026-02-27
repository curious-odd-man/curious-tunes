package com.github.curiousoddman.curious_tunes.model.playlist;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.domain.DataAccess;
import com.github.curiousoddman.curious_tunes.event.AddToPlaylistEvent;
import com.github.curiousoddman.curious_tunes.event.player.PlayerStatusEvent;
import com.github.curiousoddman.curious_tunes.model.Shuffle;
import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
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
    private final DataAccess dataAccess;

    private PlaylistItem currentlyPlayingItem = null;


    public void addItems(AddToPlaylistEvent addToPlaylistEvent) {
        List<PlaylistItem> tracksToAdd = new ArrayList<>();
        tracksToAdd.addAll(getTracks(addToPlaylistEvent));
        tracksToAdd.addAll(getTracksForArtist(addToPlaylistEvent));
        tracksToAdd.addAll(getTracksForAlbum(addToPlaylistEvent));
        boolean replacePlaylist = addToPlaylistEvent.getPlaylistAddMode() == PlaylistAddMode.REPLACE;
        if (replacePlaylist) {
            clear();
        }
        int addPosition = playlistItems.size();
        if (addToPlaylistEvent.getPlaylistAddMode() == PlaylistAddMode.PUT_AFTER_CURRENT) {
            if (currentlyPlayingItem == null) {
                addPosition = 0;
            } else {
                addPosition = playlistItems.indexOf(currentlyPlayingItem) + 1;
            }
        }
        Shuffle shuffle = addToPlaylistEvent.getShuffle();
        if (shuffle == Shuffle.SKIP) {
            playlistItems.addAll(addPosition, tracksToAdd);
        } else if (shuffle == Shuffle.BEFORE_ADDING_TO_PLAYLIST) {
            Collections.shuffle(tracksToAdd);
            playlistItems.addAll(addPosition, tracksToAdd);
        } else if (shuffle == Shuffle.AFTER_ADDING_TO_PLAYLIST) {
            playlistItems.addAll(addPosition, tracksToAdd);
            Collections.shuffle(playlistItems);
        } else {
            log.error("Unknown type of shuffle {}", shuffle);
            playlistItems.addAll(addPosition, tracksToAdd);
        }
        if (currentlyPlayingItem != null) {
            if (!replacePlaylist) {
                playlistItems.remove(currentlyPlayingItem);
            }
            playlistItems.add(Math.max(addPosition - 1, 0), currentlyPlayingItem);
        }
        selectionModel.clearSelection();
    }

    private List<PlaylistItem> getTracksForAlbum(AddToPlaylistEvent addToPlaylistEvent) {
        List<AlbumRecord> albums = addToPlaylistEvent.getAlbums();
        if (albums == null || albums.isEmpty()) {
            return List.of();
        }

        List<TrackInfo> albumsTracks = dataAccess.getAlbumsTracks(albums);
        Set<Integer> artistFks = albumsTracks
                .stream()
                .map(TrackInfo::getTrackAlbum)
                .map(AlbumRecord::getFkArtist)
                .collect(Collectors.toSet());
        Map<Integer, ArtistRecord> artistRecordMap = dataAccess
                .getArtists(artistFks)
                .stream()
                .collect(Collectors.toMap(ArtistRecord::getId, Function.identity()));
        return albumsTracks
                .stream()
                .map(trackInfo -> new PlaylistItem(
                        trackInfo.getTrackRecord(),
                        artistRecordMap.get(trackInfo.getTrackAlbum().getFkArtist()),
                        trackInfo.getTrackAlbum()))
                .toList();
    }

    private List<PlaylistItem> getTracksForArtist(AddToPlaylistEvent addToPlaylistEvent) {
        if (addToPlaylistEvent.getArtistRecord() == null) {
            return List.of();
        }
        List<TrackInfo> artistTracks = dataAccess.getArtistTracks(addToPlaylistEvent.getArtistRecord());
        return artistTracks
                .stream()
                .map(trackInfo -> new PlaylistItem(
                        trackInfo.getTrackRecord(),
                        addToPlaylistEvent.getArtistRecord(),
                        trackInfo.getTrackAlbum()))
                .toList();
    }

    private static List<PlaylistItem> getTracks(AddToPlaylistEvent addToPlaylistEvent) {
        if (addToPlaylistEvent.getTracks() == null) {
            return List.of();
        }
        return addToPlaylistEvent
                .getTracks()
                .stream()
                .map(tqi -> new PlaylistItem(tqi.getTrackRecord(), tqi.getTrackArtist(), tqi.getTrackAlbum()))
                .toList();
    }

    public void clear() {
        selectionModel.clearSelection();
        currentlyPlayingItem = null;
        playlistItems.clear();
    }

    public OptionalInt moveSelectedUp() {
        Optional<PlaylistItem> current = Optional.ofNullable(currentlyPlayingItem);
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
        int selectedIndex = selectionModel.getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= playlistItems.size()) {
            return OptionalInt.empty();
        }

        int newSelectedIndex = selectedIndex + 1;
        swap(playlistItems, selectedIndex, newSelectedIndex);
        selectionModel.select(newSelectedIndex);
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
        Optional<PlaylistItem> current = Optional.ofNullable(currentlyPlayingItem);
        current.ifPresent(playlistItems::remove);
        Collections.shuffle(playlistItems);
        selectionModel.clearSelection();
        current.ifPresent(playlistItems::addFirst);
        logPlaylistItems();
    }

    public Optional<PlaylistItem> getNextForPlayback() {
        logPlaylistItems();

        int nextIndex = currentlyPlayingItem == null
                ? 0
                : (playlistItems.indexOf(currentlyPlayingItem) + 1);

        if (nextIndex < playlistItems.size()) {
            PlaylistItem playlistItem = playlistItems.get(nextIndex);
            log.info("▶ Next item to play is: {}", playlistItem.getTrackRecord().getTitle());
            return Optional.of(playlistItem);
        }
        return Optional.empty();
    }

    private void logPlaylistItems() {
        MDC.put(".", "PLAYLIST");
        log.debug("Dumping playlist -------------------- {} items", playlistItems.size());
        int selectedIndex = selectionModel.getSelectedIndex();
        for (int i = 0; i < playlistItems.size(); i++) {
            PlaylistItem playlistItem = playlistItems.get(i);
            log.debug(
                    "\t {}{} {} {}",
                    selectedIndex == i ? "S" : " ",
                    playlistItem.equals(currentlyPlayingItem) ? "P" : " ",
                    playlistItem.getPlaylistItemStatus(),
                    playlistItem.getTrackRecord().getTitle()
            );
        }
        log.debug("END");
        MDC.remove(".");
    }

    public ReadOnlyObjectProperty<PlaylistItem> getSelectedProperty() {
        return selectionModel.selectedItemProperty();
    }

    public void select(PlaylistItem playlistItem) {
        selectionModel.select(playlistItem);
    }

    public void updateStatus(PlayerStatusEvent playerStatusEvent) {
        PlaylistItem playlistItem = playerStatusEvent.getPlaylistItem();
        switch (playerStatusEvent.getStatus()) {
            case PAUSED, PLAYING -> {
            }
            case LAUNCHING -> {
                playlistItem.setPlaylistItemStatus(PlaylistItemStatus.PLAYING);
                currentlyPlayingItem = playlistItem;
            }
            case STOPPED, ENDED -> playlistItem.setPlaylistItemStatus(PlaylistItemStatus.PLAYED);
        }
    }

    public Optional<PlaylistItem> getCurrentlyPlayingItem() {
        return Optional.ofNullable(currentlyPlayingItem);
    }
}
