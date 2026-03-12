package com.github.curiousoddman.curious_tunes.model.playlist;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.domain.DataAccess;
import com.github.curiousoddman.curious_tunes.event.AddToPlaylistEvent;
import com.github.curiousoddman.curious_tunes.event.player.PlayerStatusEvent;
import com.github.curiousoddman.curious_tunes.model.Shuffle;
import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
import javafx.beans.InvalidationListener;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

// AI Generated
@Slf4j
@Component
@RequiredArgsConstructor
public class PlaylistModel {
    @Getter
    private final List<PlaylistItem> playlistItems = new ArrayList<>();
    @Getter
    private final PlaylistMultiSelectionModel selectionModel = new PlaylistMultiSelectionModel(playlistItems);
    private final DataAccess dataAccess;

    private PlaylistItem currentlyPlayingItem = null;

    public void addItems(AddToPlaylistEvent addToPlaylistEvent) {
        selectionModel.getSelectedIndices().addListener((InvalidationListener) o -> {
            logPlaylistItems();
        });
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
        if (shuffle == Shuffle.BEFORE_ADDING_TO_PLAYLIST) {
            Collections.shuffle(tracksToAdd);
        }

        playlistItems.addAll(addPosition, tracksToAdd);

        if (shuffle == Shuffle.AFTER_ADDING_TO_PLAYLIST) {
            shuffleKeepingCurrent();
        }

        selectionModel.clearSelection();
        logPlaylistItems();
    }

    public void clear() {
        selectionModel.clearSelection();
        currentlyPlayingItem = null;
        playlistItems.clear();
        logPlaylistItems();
    }

    public List<PlaylistItemMovement> moveSelectedUp() {
        List<Integer> sortedSelectedIndices = selectionModel.getSortedSelectedIndices();
        if (sortedSelectedIndices.isEmpty()) {
            return List.of();
        }

        // Pin the currently-playing item so it is never displaced
        int currentlyPlayingIndex = currentlyPlayingItem == null
                ? -1
                : playlistItems.indexOf(currentlyPlayingItem);

        int floor = (currentlyPlayingIndex >= 0)
                ? currentlyPlayingIndex + 1
                : 0;
        if (sortedSelectedIndices.getFirst() <= floor) {
            return List.of();
        }

        List<PlaylistItemMovement> movements = new ArrayList<>();

        // Move each selected item up, from top to bottom to avoid conflicts
        for (int idx : sortedSelectedIndices) {
            int target = idx - 1;
            if (target >= floor) {
                Collections.swap(playlistItems, idx, target);
                selectionModel.notifySwap(idx, target);
                movements.add(new PlaylistItemMovement(idx, target));
            }
        }

        logPlaylistItems();
        return movements;
    }

    public List<PlaylistItemMovement> moveSelectedDown() {
        List<Integer> sorted = selectionModel.getSortedSelectedIndices();
        if (sorted.isEmpty()) {
            return List.of();
        }

        int ceiling = playlistItems.size() - 1;

        // Cannot move if the bottom-most selected item is already at the bottom
        if (sorted.getLast() >= ceiling) {
            return List.of();
        }

        List<PlaylistItemMovement> movements = new ArrayList<>();

        // Move each selected item down, from bottom to top to avoid conflicts
        List<Integer> reversed = new ArrayList<>(sorted);
        Collections.reverse(reversed);
        for (int idx : reversed) {
            int target = idx + 1;
            if (target <= ceiling && !sorted.contains(target)) {
                Collections.swap(playlistItems, idx, target);
                selectionModel.notifySwap(idx, target);
                movements.add(new PlaylistItemMovement(idx, target));
            }
        }

        logPlaylistItems();
        return movements;
    }

    public void moveItemTo(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return;
        }
        if (fromIndex < 0 || fromIndex >= playlistItems.size()) {
            return;
        }
        if (toIndex < 0 || toIndex >= playlistItems.size()) {
            return;
        }

        // Protect the currently-playing item from being moved
        if (playlistItems.get(fromIndex).equals(currentlyPlayingItem)) {
            return;
        }

        List<Integer> indicesToMove;
        if (selectionModel.isSelected(fromIndex)) {
            indicesToMove = selectionModel.getSortedSelectedIndices();
            // Remove the pinned item from the block if it sneaked in
            int pinnedIndex = currentlyPlayingItem == null ? -1
                    : playlistItems.indexOf(currentlyPlayingItem);
            indicesToMove = indicesToMove.stream()
                    .filter(i -> i != pinnedIndex)
                    .collect(Collectors.toCollection(ArrayList::new));
        } else {
            indicesToMove = List.of(fromIndex);
        }

        // Extract items, erase from list, re-insert at adjusted target
        List<PlaylistItem> itemsToMove = indicesToMove.stream()
                .map(playlistItems::get)
                .toList();

        // Count how many selected items sit before toIndex (they shift toIndex left)
        long before = indicesToMove.stream().filter(i -> i < toIndex).count();
        int adjustedTo = (int) (toIndex - before);

        playlistItems.removeAll(itemsToMove);
        adjustedTo = Math.max(0, Math.min(adjustedTo, playlistItems.size()));
        playlistItems.addAll(adjustedTo, itemsToMove);

        // Re-select the moved items at their new positions
        selectionModel.clearSelection();
        for (int i = 0; i < itemsToMove.size(); i++) {
            selectionModel.toggleSelect(adjustedTo + i);
        }

        logPlaylistItems();
    }

    public OptionalInt deleteSelected() {
        int selectedIndex = selectionModel.getSelectedIndex();
        if (selectedIndex < 0) return OptionalInt.empty();

        PlaylistItem item = playlistItems.get(selectedIndex);
        playlistItems.remove(selectedIndex);
        selectionModel.notifyRemoved(selectedIndex);

        // If this was the currently-playing item, clear it
        if (item.equals(currentlyPlayingItem)) {
            currentlyPlayingItem = null;
        }

        logPlaylistItems();
        return OptionalInt.of(selectedIndex);
    }

    public List<Integer> deleteAllSelected() {
        List<Integer> sorted = selectionModel.getSortedSelectedIndices();
        if (sorted.isEmpty()) return List.of();

        // Remove from back to front so indices stay valid
        List<Integer> reversed = new ArrayList<>(sorted);
        Collections.reverse(reversed);
        for (int idx : reversed) {
            PlaylistItem item = playlistItems.get(idx);
            playlistItems.remove(idx);
            if (item.equals(currentlyPlayingItem)) {
                currentlyPlayingItem = null;
            }
        }
        selectionModel.clearSelection();
        logPlaylistItems();
        return sorted;
    }

    public void shuffle() {
        shuffleKeepingCurrent();
        selectionModel.clearSelection();
        logPlaylistItems();
    }

    private void shuffleKeepingCurrent() {
        if (currentlyPlayingItem != null) {
            playlistItems.remove(currentlyPlayingItem);
        }
        Collections.shuffle(playlistItems);
        if (currentlyPlayingItem != null) {
            playlistItems.addFirst(currentlyPlayingItem);
        }
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

    public void updateStatus(PlayerStatusEvent playerStatusEvent) {
        PlaylistItem playlistItem = playerStatusEvent.getPlaylistItem();
        switch (playerStatusEvent.getStatus()) {
            case PAUSED, PLAYING -> { /* no structural change */ }
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

    // =========================================================================
    // Selection delegation
    // =========================================================================

    public PlaylistItem getSelected() {
        return selectionModel.getSelectedItem();
    }

    public void selectOnly(PlaylistItem playlistItem) {
        selectionModel.clearAndSelect(playlistItem);
    }

    public void toggleSelect(PlaylistItem index) {
        selectionModel.toggleSelect(index);
    }

    public boolean isSelected(PlaylistItem index) {
        return selectionModel.isSelected(index);
    }

    private List<PlaylistItem> getTracksForAlbum(AddToPlaylistEvent addToPlaylistEvent) {
        List<AlbumRecord> albums = addToPlaylistEvent.getAlbums();
        if (albums == null || albums.isEmpty()) {
            return List.of();
        }

        List<TrackInfo> albumsTracks = dataAccess.getAlbumsTracks(albums);
        Set<Integer> artistFks = albumsTracks.stream()
                .map(TrackInfo::getTrackAlbum)
                .map(AlbumRecord::getFkArtist)
                .collect(Collectors.toSet());
        Map<Integer, ArtistRecord> artistRecordMap = dataAccess.getArtists(artistFks).stream()
                .collect(Collectors.toMap(ArtistRecord::getId, Function.identity()));
        return albumsTracks.stream()
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
        return artistTracks.stream()
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
        return addToPlaylistEvent.getTracks().stream()
                .map(tqi -> new PlaylistItem(
                        tqi.getTrackRecord(), tqi.getTrackArtist(), tqi.getTrackAlbum()))
                .toList();
    }

    private void logPlaylistItems() {
        if (log.isDebugEnabled()) {
            String callerMethod = StackWalker.getInstance()
                    .walk(frames -> frames.skip(1).findFirst())
                    .map(StackWalker.StackFrame::getMethodName)
                    .orElse("unknown");

            MDC.put(".", "PLAYLIST");
            log.debug("Dumping playlist -------------------- {} items...  from {}", playlistItems.size(), callerMethod);
            List<Integer> selectedIndices = selectionModel.getSortedSelectedIndices();
            for (int i = 0; i < playlistItems.size(); i++) {
                PlaylistItem pi = playlistItems.get(i);
                log.debug("\t {}{} {} {}",
                        selectedIndices.contains(i) ? "S" : " ",
                        pi.equals(currentlyPlayingItem) ? "P" : " ",
                        pi.getPlaylistItemStatus(),
                        pi.getTrackRecord().getTitle());
            }
            log.debug("END");
            MDC.remove(".");
        }
    }

    public void rangeSelect(PlaylistItem toIndex) {
        selectionModel.rangeSelect(toIndex);
    }
}