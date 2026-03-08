package com.github.curiousoddman.curious_tunes;

import com.github.curiousoddman.curious_tunes.dbobj.tables.records.AlbumRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.ArtistRecord;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.TrackRecord;
import com.github.curiousoddman.curious_tunes.domain.DataAccess;
import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
import com.github.curiousoddman.curious_tunes.ui.controller.element.tabs.LibraryTagEditTabController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.github.curiousoddman.curious_tunes.dbobj.Tables.TRACK;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;


// AI Generated
@ExtendWith(MockitoExtension.class)
class TrackEditControllerSaveTest {

    // ── JavaFX bootstrap ─────────────────────────────────────────────────────

    @BeforeAll
    static void initJfx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(latch::countDown);
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX failed to start");
    }

    // ── Mocks ────────────────────────────────────────────────────────────────

    @Mock
    DataAccess dataAccess;
    @Mock
    TrackInfo trackInfo;
    @Mock
    TrackRecord trackRecord;
    @Mock
    ArtistRecord existingArtist;
    @Mock
    ArtistRecord newArtist;
    @Mock
    AlbumRecord existingAlbum;
    @Mock
    AlbumRecord newAlbum;

    // ── System under test ────────────────────────────────────────────────────

    LibraryTagEditTabController sut;

    // ── Setup ────────────────────────────────────────────────────────────────

    @BeforeEach
    void setup() throws Exception {
        // Common stubs
        lenient().when(trackInfo.getTrackRecord()).thenReturn(trackRecord);
        lenient().when(trackInfo.getTrackArtist()).thenReturn(existingArtist);
        lenient().when(trackInfo.getAlbumImage()).thenReturn(null);
        lenient().when(existingArtist.getId()).thenReturn(1);
        lenient().when(existingAlbum.getId()).thenReturn(10);
        lenient().when(trackRecord.getFkAlbum()).thenReturn(10);

        // Default: UI fields match trackInfo — no changes
        lenient().when(trackInfo.getArtistName()).thenReturn("Artist");
        lenient().when(trackInfo.getAlbumName()).thenReturn("Album");
        lenient().when(trackInfo.getTitle()).thenReturn("Title");
        lenient().when(trackInfo.getTrackNumber()).thenReturn(1);
        lenient().when(trackInfo.getDiskNumber()).thenReturn(1);
        lenient().when(trackInfo.getGenre()).thenReturn("Rock");
        lenient().when(trackInfo.getComposer()).thenReturn("Composer");
        lenient().when(trackInfo.getReleaseDate()).thenReturn("2020");
        lenient().when(trackInfo.getLyrics()).thenReturn("Lyrics");

        runOnJfxThread(() -> {
            sut = new LibraryTagEditTabController(mock(), dataAccess);

            // Wire up real UI fields
            sut.artistField = new TextField();
            sut.albumField = new TextField();
            sut.titleField = new TextField();
            sut.trackNumberField = new TextField();
            sut.diskNumberField = new TextField();
            sut.genreField = new TextField();
            sut.composerField = new TextField();
            sut.releaseDateField = new TextField();
            sut.lyricsEditArea = new TextArea();

            sut.showTags(trackInfo);
        });
    }

    // ── Artist changed ───────────────────────────────────────────────────────

    @Test
    void artistChanged_createsNewArtistAndAlbum() throws Exception {
        when(dataAccess.getOrInsertArtist("New Artist")).thenReturn(newArtist);
        when(newArtist.getId()).thenReturn(2);
        when(dataAccess.getOrInsertAlbum(2, "Album", null)).thenReturn(newAlbum);
        when(newAlbum.getId()).thenReturn(20);

        runOnJfxThread(() -> {
            sut.artistField.setText("New Artist");
            sut.onSave(new ActionEvent());
        });

        verify(dataAccess).getOrInsertArtist("New Artist");
        verify(dataAccess).getOrInsertAlbum(2, "Album", null);
        verify(dataAccess).storeTrackOverride(trackInfo, TRACK.FK_ALBUM, "10");
        verify(trackRecord).setFkAlbum(20);
        verify(trackRecord).update(TRACK.FK_ALBUM);
    }

    @Test
    void artistChanged_albumBranchNotEntered() throws Exception {
        when(dataAccess.getOrInsertArtist("New Artist")).thenReturn(newArtist);
        when(newArtist.getId()).thenReturn(2);
        when(dataAccess.getOrInsertAlbum(anyInt(), any(), any())).thenReturn(newAlbum);
        when(newAlbum.getId()).thenReturn(20);

        runOnJfxThread(() -> {
            sut.artistField.setText("New Artist");
            sut.onSave(new ActionEvent());
        });

        // album-only branch must not also fire
        verify(dataAccess, times(1)).getOrInsertAlbum(anyInt(), anyString(), any());
    }

    // ── Album changed (artist unchanged) ────────────────────────────────────

    @Test
    void albumChanged_artistUnchanged_createsNewAlbumUnderExistingArtist() throws Exception {
        when(dataAccess.getOrInsertAlbum(1, "New Album", null)).thenReturn(newAlbum);
        when(newAlbum.getId()).thenReturn(20);

        runOnJfxThread(() -> {
            sut.albumField.setText("New Album");
            sut.onSave(new ActionEvent());
        });

        verify(dataAccess, never()).getOrInsertArtist(any());
        verify(dataAccess).getOrInsertAlbum(1, "New Album", null);
        verify(dataAccess).storeTrackOverride(trackInfo, TRACK.FK_ALBUM, "10");
        verify(trackRecord).setFkAlbum(20);
    }

    // ── Simple field changes ─────────────────────────────────────────────────

    @Test
    void titleChanged_storesOverrideAndUpdatesRecord() throws Exception {
        runOnJfxThread(() -> {
            sut.titleField.setText("New Title");
            sut.onSave(new ActionEvent());
        });

        verify(dataAccess).storeTrackOverride(trackInfo, TRACK.TITLE, "Title");
        verify(trackRecord).set(TRACK.TITLE, "New Title");
    }

    @Test
    void trackNumberChanged_convertsToInteger() throws Exception {
        when(trackInfo.getTrackNumber()).thenReturn(3);

        runOnJfxThread(() -> {
            sut.trackNumberField.setText("5");
            sut.onSave(new ActionEvent());
        });

        verify(dataAccess).storeTrackOverride(trackInfo, TRACK.TRACK_NUMBER, "3");
        verify(trackRecord).set(TRACK.TRACK_NUMBER, 5);
    }

    @Test
    void genreChanged_storesOverride() throws Exception {
        runOnJfxThread(() -> {
            sut.genreField.setText("Jazz");
            sut.onSave(new ActionEvent());
        });

        verify(dataAccess).storeTrackOverride(trackInfo, TRACK.GENRE, "Rock");
        verify(trackRecord).set(TRACK.GENRE, "Jazz");
    }

    @Test
    void lyricsChanged_storesOverride() throws Exception {
        runOnJfxThread(() -> {
            sut.lyricsEditArea.setText("New lyrics");
            sut.onSave(new ActionEvent());
        });

        verify(dataAccess).storeTrackOverride(trackInfo, TRACK.LYRICS, "Lyrics");
        verify(trackRecord).set(TRACK.LYRICS, "New lyrics");
    }

    // ── No changes ───────────────────────────────────────────────────────────

    @Test
    void noFieldsChanged_noOverridesStored() throws Exception {
        runOnJfxThread(() -> sut.onSave(new ActionEvent()));

        verify(dataAccess, never()).storeTrackOverride(any(), any(), any());
        verify(dataAccess, never()).getOrInsertArtist(any());
        verify(dataAccess, never()).getOrInsertAlbum(anyInt(), any(), any());
    }

    // ── Multiple fields changed ──────────────────────────────────────────────

    @Test
    void multipleFieldsChanged_allOverridesStored() throws Exception {
        runOnJfxThread(() -> {
            sut.titleField.setText("New Title");
            sut.genreField.setText("Jazz");
            sut.composerField.setText("New Composer");
            sut.onSave(new ActionEvent());
        });

        verify(dataAccess, times(2)).getTrackOverrides(anyInt());
        verify(dataAccess).storeTrackOverride(trackInfo, TRACK.TITLE, "Title");
        verify(dataAccess).storeTrackOverride(trackInfo, TRACK.GENRE, "Rock");
        verify(dataAccess).storeTrackOverride(trackInfo, TRACK.COMPOSER, "Composer");
        verifyNoMoreInteractions(dataAccess);
    }

    // ── Null / empty edge cases ──────────────────────────────────────────────

    @Test
    void fieldWasNull_userEntersValue_storesOverride() throws Exception {
        when(trackInfo.getGenre()).thenReturn(null);

        runOnJfxThread(() -> {
            sut.genreField.setText("Rock");
            sut.onSave(new ActionEvent());
        });

        verify(dataAccess).storeTrackOverride(trackInfo, TRACK.GENRE, null);
    }

    @Test
    void fieldHadValue_userClearsIt_storesOverride() throws Exception {
        runOnJfxThread(() -> {
            sut.genreField.setText("");
            sut.onSave(new ActionEvent());
        });

        verify(dataAccess).storeTrackOverride(trackInfo, TRACK.GENRE, "Rock");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static void runOnJfxThread(Runnable action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            action.run();
            latch.countDown();
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS), "JavaFX thread timed out");
    }
}