package com.github.curiousoddman.curious_tunes.model.playlist;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

// AI Generated
@Getter
public class PlaylistMultiSelectionModel extends MultipleSelectionModel<PlaylistItem> {
    private final List<PlaylistItem> playlistItems;

    private final ObservableList<Integer> selectedIndices = FXCollections.observableArrayList();
    private final ObservableList<PlaylistItem> selectedItems = FXCollections.observableArrayList();

    public PlaylistMultiSelectionModel(List<PlaylistItem> playlistItems) {
        this.playlistItems = playlistItems;
        setSelectionMode(SelectionMode.MULTIPLE);
    }

    @Override
    public void select(int index) {
        if (index < 0 || index >= playlistItems.size()) {
            return;
        }
        if (!selectedIndices.contains(index)) {
            selectedIndices.add(index);
            FXCollections.sort(selectedIndices);
            selectedItems.setAll(selectedIndices.stream().map(playlistItems::get).toList());
        }
        // Always update the single-item selectedIndex / selectedItem properties
        setSelectedIndex(index);
        setSelectedItem(playlistItems.get(index));
    }

    @Override
    public void select(PlaylistItem item) {
        int index = playlistItems.indexOf(item);
        if (index >= 0) {
            select(index);
        }
    }

    @Override
    public void clearAndSelect(int index) {
        clearSelection();
        select(index);
    }

    public void clearAndSelect(PlaylistItem playlistItem) {
        clearSelection();
        select(playlistItem);
    }

    @Override
    public void clearSelection(int index) {
        if (selectedIndices.remove(Integer.valueOf(index))) {
            selectedItems.setAll(selectedIndices.stream().map(playlistItems::get).toList());
            if (getSelectedIndex() == index) {
                // Move primary selection to the last remaining item, or clear it
                if (selectedIndices.isEmpty()) {
                    setSelectedIndex(-1);
                    setSelectedItem(null);
                } else {
                    int last = selectedIndices.getLast();
                    setSelectedIndex(last);
                    setSelectedItem(playlistItems.get(last));
                }
            }
        }
    }

    @Override
    public void clearSelection() {
        selectedIndices.clear();
        selectedItems.clear();
        setSelectedIndex(-1);
        setSelectedItem(null);
    }

    @Override
    public void selectIndices(int index, int... remainder) {
        if (remainder == null) {
            select(index);
            return;
        }

        int[] indexArr = new int[remainder.length + 1];
        indexArr[0] = index;
        System.arraycopy(remainder, 0, indexArr, 1, remainder.length);

        List<Integer> validIndexes = new ArrayList<>();
        for (int currIdx : indexArr) {
            if (currIdx < 0 || currIdx >= playlistItems.size()) {
                continue;
            }
            if (!selectedIndices.contains(index)) {
                validIndexes.add(currIdx);
            }
        }

        if (!validIndexes.isEmpty()) {
            selectedIndices.addAll(validIndexes);
            FXCollections.sort(selectedIndices);
            selectedItems.setAll(selectedIndices.stream().map(playlistItems::get).toList());

            setSelectedIndex(validIndexes.getFirst());
            setSelectedItem(playlistItems.get(validIndexes.getFirst()));
        }
    }

    @Override
    public void selectRange(int start, int end) {
        int from = Math.min(start, end);
        int to = Math.max(start, end);
        selectIndices(from, IntStream.range(from + 1, to).toArray());
    }

    public void selectRangeInclusive(int from, int to) {
        selectRange(Math.min(from, to), Math.max(from, to) + 1);
    }

    @Override
    public void selectAll() {
        selectRange(0, playlistItems.size());
    }

    @Override
    public void selectFirst() {
        if (!playlistItems.isEmpty()) {
            clearAndSelect(0);
        }
    }

    @Override
    public void selectLast() {
        if (!playlistItems.isEmpty()) {
            clearAndSelect(playlistItems.size() - 1);
        }
    }

    @Override
    public void selectPrevious() {
        int current = getSelectedIndex();
        if (current > 0) {
            clearAndSelect(current - 1);
        }
    }

    @Override
    public void selectNext() {
        int current = getSelectedIndex();
        if (current >= 0 && current < playlistItems.size() - 1) {
            clearAndSelect(current + 1);
        }
    }

    @Override
    public boolean isSelected(int index) {
        return selectedIndices.contains(index);
    }

    public boolean isSelected(PlaylistItem playlistItem) {
        return selectedItems.contains(playlistItem);
    }

    @Override
    public boolean isEmpty() {
        return selectedIndices.isEmpty();
    }

    public Optional<PlaylistItem> getOptionalSelectedItem() {
        return Optional.ofNullable(getSelectedItem());
    }

    public List<Integer> getSortedSelectedIndices() {
        List<Integer> copy = new ArrayList<>(selectedIndices);
        Collections.sort(copy);
        return copy;
    }

    public void notifySwap(int i1, int i2) {
        boolean sel1 = selectedIndices.contains(i1);
        boolean sel2 = selectedIndices.contains(i2);
        if (sel1 == sel2) {
            return;
        }

        if (sel1) {
            selectedIndices.remove(Integer.valueOf(i1));
            selectedIndices.add(i2);
        } else {
            selectedIndices.remove(Integer.valueOf(i2));
            selectedIndices.add(i1);
        }
        FXCollections.sort(selectedIndices);
        selectedItems.setAll(selectedIndices.stream().map(playlistItems::get).toList());

        int primary = getSelectedIndex();
        if (primary == i1) {
            setSelectedIndex(i2);
        } else if (primary == i2) {
            setSelectedIndex(i1);
        }
        setSelectedItem(getSelectedIndex() >= 0 ? playlistItems.get(getSelectedIndex()) : null);
    }

    public void notifyRemoved(int removedIndex) {
        List<Integer> updated = new ArrayList<>();
        for (int idx : selectedIndices) {
            if (idx == removedIndex) {
                continue;
            }
            updated.add(idx > removedIndex ? idx - 1 : idx);
        }
        selectedIndices.setAll(updated);
        selectedItems.setAll(selectedIndices.stream().map(playlistItems::get).toList());

        int primary = getSelectedIndex();
        if (primary == removedIndex) {
            int next = updated.isEmpty() ? -1 : updated.getFirst();
            setSelectedIndex(next);
            setSelectedItem(next >= 0 ? playlistItems.get(next) : null);
        } else if (primary > removedIndex) {
            setSelectedIndex(primary - 1);
        }
    }

    public void toggleSelect(PlaylistItem playlistItem) {
        toggleSelect(playlistItems.indexOf(playlistItem));
    }

    public void toggleSelect(int index) {
        if (isSelected(index)) {
            clearSelection(index);
        } else {
            select(index);
        }
    }

    /**
     * Shift+click style: extend selection from the current primary index to playlistItem.
     * If nothing is selected yet, behaves like a plain select.
     * The primary (anchor) index is intentionally not moved — repeated Shift+clicks
     * always extend/shrink relative to the same anchor.
     */
    public void rangeSelect(PlaylistItem playlistItem) {
        int anchor = getSelectedIndex();
        if (anchor < 0) {
            clearAndSelect(playlistItem);
            return;
        }
        int index = playlistItems.indexOf(playlistItem);
        // Clear only the previous range, then reselect from anchor to new playlistItem
        clearSelection();
        selectRangeInclusive(anchor, index);
        // Restore anchor as the primary index so further Shift+clicks stay anchored
        setSelectedIndex(anchor);
        setSelectedItem(playlistItems.get(anchor));
    }
}
