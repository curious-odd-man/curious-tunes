package com.github.curiousoddman.curious_tunes.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecentFoldersTest {
    @Test
    void newFolderGoesFirst() {
        assertEquals(List.of("C:\\c", "C:\\a", "C:\\b"), RecentFolders.push(List.of("C:\\a", "C:\\b"), "C:\\c", 8, 250));
    }

    @Test
    void aFolderThatIsAlreadyThereMovesToTheFront() {
        assertEquals(List.of("C:\\b", "C:\\a"), RecentFolders.push(List.of("C:\\a", "C:\\b"), "C:\\b", 8, 250));
    }

    @Test
    void numberOfFoldersIsLimited() {
        assertEquals(List.of("4", "3"), RecentFolders.push(List.of("3", "2", "1"), "4", 2, 250));
    }

    @Test
    void oldestFoldersAreDroppedToFitTheStoredLength() {
        List<String> result = RecentFolders.push(List.of("bbbbbbbbbb", "cccccccccc"), "aaaaaaaaaa", 8, 22);
        assertEquals(List.of("aaaaaaaaaa", "bbbbbbbbbb"), result);       // 10 + newline + 10 = 21 characters
    }

    @Test
    void theNewestFolderIsKeptEvenWhenItIsLong() {
        assertEquals(List.of("a-very-long-folder-name"), RecentFolders.push(List.of("x"), "a-very-long-folder-name", 8, 5));
    }

    @Test
    void storedTextIsOneFolderPerLine() {
        assertEquals(List.of("a", "b"), RecentFolders.parse("a\nb\n\n"));
        assertEquals("a\nb", RecentFolders.join(List.of("a", "b")));
        assertEquals(List.of(), RecentFolders.parse(null));
    }
}
