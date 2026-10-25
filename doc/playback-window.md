# Playback Window Mode — Requirements & Design Notes

Status: **IMPLEMENTED (not yet built or run on a real machine) — see section 9.**
Items marked **[PROPOSAL]** are my suggestions that have not been explicitly confirmed.
Items marked **[OPEN]** are still unanswered.

---

## 1. Goal

A separate "now playing" window, available during playback, that shows the current track in a larger, more focused
layout than the library screen. It is inspired by iTunes but is not a copy of it.

## 2. Decisions

| #  | Topic                     | Decision                                                                                                                                                                                                                                                                                                                                         |
|----|---------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | Relationship to library   | Separate window. The library window is **hidden** (not destroyed) while the playback window is open and is brought back when the playback window is left, so library state (selection, tabs, playlist, scroll) is kept.                                                                                                                          |
| 2  | Closing                   | The X button on the playback window returns to the library. It does not exit the app.                                                                                                                                                                                                                                                            |
| 3  | Views                     | Two views: **Simple** (compact) and **Maximized**. A button switches between them.                                                                                                                                                                                                                                                               |
| 4  | Maximized                 | The normal maximized window state (title bar and taskbar visible), not exclusive full screen.                                                                                                                                                                                                                                                    |
| 5  | Platform                  | Windows only.                                                                                                                                                                                                                                                                                                                                    |
| 6  | Frame                     | Normal OS window frame is kept (not frameless).                                                                                                                                                                                                                                                                                                  |
| 7  | Waveform                  | Same behavior as the library view. Click-to-seek only.                                                                                                                                                                                                                                                                                           |
| 8  | Session timer             | Wall clock. Starts at first playback. Resets when the playlist is fully replaced **or cleared**. Shows nothing until the first playback after a reset.                                                                                                                                                                                           |
| 9  | Controls                  | Play/pause, next, volume, lyrics text size. No shuffle, no previous.                                                                                                                                                                                                                                                                             |
| 10 | Next while paused         | Starts playing the next track immediately.                                                                                                                                                                                                                                                                                                       |
| 11 | Up next                   | Next track in playlist order. Artist/album that match the current track are dimmed.                                                                                                                                                                                                                                                              |
| 12 | Lyrics                    | Scrolls when text overflows. Size control is a **slider**; an **"Auto-fit" checkbox** (checked by default) controls automatic fitting. Auto-fit runs **only at track start**. Moving the slider never changes the checkbox. Nothing about lyrics size is saved. Shows stored lyrics only (no fetching). Shows a "no lyrics" message when absent. |
| 13 | Simple view content       | No lyrics, one-line "Up next" with small thumbnail.                                                                                                                                                                                                                                                                                              |
| 14 | Entry button              | On the library view, next to play/pause. Enabled only while a track is playing or paused; **disabled when stopped or ended**. No keyboard shortcut.                                                                                                                                                                                              |
| 15 | Persistence               | Window size and position are remembered, **for Simple view only**. Last-used view is not remembered (always opens in Simple).                                                                                                                                                                                                                    |
| 16 | Look                      | Colors derived from album art, including the playback window's waveform colors. Color changes between tracks are animated. The library waveform keeps its current fixed colors.                                                                                                                                                                  |
| 17 | Stop/end with window open | The playback window stays open, showing the last track.                                                                                                                                                                                                                                                                                          |
| 18 | Session timer format      | `m:ss`, switching to `h:mm:ss` after one hour.                                                                                                                                                                                                                                                                                                   |

## 3. Requirements (mapped to the original list)

| #   | Requirement                                                      |           Simple           | Maximized |
|-----|------------------------------------------------------------------|:--------------------------:|:---------:|
| R1  | Title, album, artist                                             |       yes (compact)        |    yes    |
| R2  | Seekable waveform (click to seek)                                |            yes             |    yes    |
| R3  | Elapsed time and remaining time                                  |            yes             |    yes    |
| R4  | Album art, if present (placeholder otherwise)                    |           small            |   large   |
| R5  | Lyrics, if present                                               |             no             |    yes    |
| R6  | Next track button (disabled if none)                             |            yes             |    yes    |
| R7  | Time since listening session start (hidden until first playback) |            yes             |    yes    |
| R8  | Up next: title, album, artist, art; matching artist/album dimmed | one line + small thumbnail | full card |
| R9  | Switch between Simple and Maximized with a button                |            yes             |    yes    |
| R10 | ~~Frameless~~ — frame is kept                                    |            n/a             |    n/a    |
| R11 | Button on library view to open this mode                         |            n/a             |    n/a    |
| R12 | Play/pause                                                       |            yes             |    yes    |
| R13 | Volume                                                           |            yes             |    yes    |
| R14 | Lyrics text size slider + auto-fit checkbox                      |            n/a             |    yes    |

## 4. Behavior details

**Opening and closing**

- Opening hides the library window; leaving (X button) shows it again. Playback continues uninterrupted either way.
- The playback window always opens in Simple view.
- If playback becomes stopped or ended while the window is open, the window stays open showing the last track. Next is
  disabled when there is no next item; play/pause behaves as in the library.

**Next button**

- Skips to the next playlist item; works while playing or paused (paused → starts the next track playing).
- Disabled when there is no next item.

**Up next**

- Shows the next playlist item (the same one already preloaded for the transition).
- Artist and album text are dimmed when equal to the current track's. Title is never dimmed.
- When there is no next track: empty-state message (e.g. "Nothing up next") and Next is disabled.

**Session timer**

- Starts at first playback; keeps running through pauses and seeks (wall clock).
- Resets when the playlist is fully replaced (`PlaylistAddMode.REPLACE`) or cleared (`PlaylistModel.clear()`). It then
  starts again at the next playback.
- Adding or inserting tracks, or playing a specific track (`PUT_AFTER_CURRENT`), does not reset it.
- Before the first playback after a reset, the timer area is empty.
- Display format: `m:ss`, switching to `h:mm:ss` after one hour.

**Lyrics**

- Source: lyrics already stored on the track. No fetching.
- Missing lyrics: a "No lyrics" message.
- Text size control: a slider. Auto-fit: a checkbox, checked by default each time the window opens.
- **Auto-fit runs only when a track starts**, and only if the checkbox is checked. It fits the lyrics to the pane.
- The slider is always usable. Moving it changes the size immediately and does **not** touch the checkbox. The size
  stays as set for the rest of the current track.
- At the next track start: if the checkbox is still checked, lyrics are auto-fitted again (overriding the slider
  position); if the user unchecked it, the slider's current size is kept.
- Nothing about lyrics size or the checkbox is saved between sessions.
- **[PROPOSAL]** After an auto-fit, the slider moves to show the fitted size, so it always reflects the current size.
- **[PROPOSAL]** Re-checking the box does not re-fit immediately; fitting happens at the next track start (consistent
  with "only on track start").
- **[PROPOSAL]** Edge case: lyrics are not visible in Simple view, so a track may start while the pane is hidden or a
  different size. Auto-fit also runs when the window is opened or switched to Maximized mid-track, if the box is
  checked (treated like a track start for fitting purposes).

**Volume**

- Shares the same value as the library volume slider (changing one moves the other). Saved to preferences the same way
  as the library slider.

**Window state**

- Size and position of the Simple view are saved to user preferences (separate from the library window's).
- Maximized is a window state only; nothing about it is saved.

**Colors from album art**

- A palette (e.g. dominant color) is derived from the album art for the window background, with text color chosen for
  readable contrast. The waveform colors (played/unplayed) also follow the palette.
- Color changes on track change are animated (e.g. a short cross-fade).
- No art: fall back to a default dark palette.
- The library's waveform keeps its current fixed colors; only the playback window recolors.

## 5. Technical findings that shape the design

These come from reading the current code.

1. **Waveform is built live and held in a singleton.** `ProgressCanvasController` is a Spring singleton bound to one
   `Canvas`. It fills the bars from `AudioSpectrumListener` data during playback, and the unheard part of a track is
   empty. It is not stored. A second canvas needs the waveform data separated from rendering (shared waveform model, two
   renderers). Recoloring from the album art also needs the colors to become parameters of the renderer.
2. **`AudioPlayer.linkWithUi(...)` accepts a single progress listener and a single waveform controller.** The playback
   window needs the same updates, so this must become multi-listener (or move to observable properties on a shared
   playback model).
3. **No skip logic exists.** A track advances only when the media ends (`onEndOfMedia` → `PlayPauseEvent` with status
   `ENDED`). Next needs a new event/handler that reuses the already preloaded `nextPlayer`/`nextPlaylistItem`. Edge
   cases: no next item, pressing Next quickly, Next while paused or while still `LAUNCHING`.
4. **Library header state is set inside `LibraryController`'s status listener.** Title, album, artist, times and lyrics
   are pushed straight into library labels. A shared playback view-model would avoid duplicating this in the new window.
5. **Single `Stage` in `StageManager`.** The playback window will be a second `Stage`. Its close request must hide it
   and show the library, and must not publish `UserShutdownApplication` (the primary stage's handler does).
6. **Session timer is new.** `MediaPlayerListeningTracker` only counts listened time per track. The session timer needs
   its own component, driven by playback status and playlist replace/clear events.
7. **Album art** is `byte[]` on the album record; `noimage.png` exists as the placeholder. Palette extraction should run
   once per track change, off the UI thread if it is slow.
8. **Preferences** go through `UserPreferencesService` / `UserPrefKey`. New keys are needed for the window's size and
   position (and lyrics size if remembered). Need to check whether a Flyway migration is required.
9. **Hidden primary stage:** `LibraryController.setUserPrefs` saves the primary stage's size and position on change.
   Hiding and re-showing must not overwrite saved library window state.

## 6. Out of scope (for now)

- Frameless window, exclusive full screen, shuffle, previous track, keyboard shortcuts.
- Lyrics fetching or editing from this window.
- Remembering the last-used view.
- Timed/synced lyrics.
- A Next button on the library view (requested only for the new window).

## 7. Suggested implementation order (once the document is approved)

1. Shared playback model (current item, next item, status, times) and multi-listener support in `AudioPlayer`.
2. Skip-to-next event and handler.
3. Session timer component.
4. Waveform split into model and renderer (colors as parameters).
5. Playback window FXML/controller (Simple layout), then Maximized layout and view switch.
6. Up next panel, lyrics pane, text size control.
7. Album-art palette and color transitions.
8. Library button, hide/show handoff, window state persistence.

## 8. Open questions

None. Section 4 contains three **[PROPOSAL]** items on lyrics auto-fit edge cases; if you don't object, they stand.

---

## 9. Implementation notes

### What was built

| Area                                                                 | Change                                                                                                                                                                                                |
|----------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ui/playback/PlaybackWindow` (new)                                   | The window: stage, Simple and Maximized layouts, controls, up next, lyrics, palette animation, window bounds. Built in code (not FXML) because both views rearrange the same nodes.                   |
| `ui/playback/PlaybackWaveformView` (new)                             | Draws the shared waveform, click to seek, palette colors. Redraws only when something changed.                                                                                                        |
| `ui/playback/PlaybackPalette`, `DominantColor`, `LyricsFitter` (new) | Album-art colors (dark theme, light text always), dominant color picking, lyrics auto-fit search.                                                                                                     |
| `model/WaveformModel` (new)                                          | Waveform data moved out of `ProgressCanvasController` so both windows share it. Library rendering/colors unchanged.                                                                                   |
| `domain/player/PlaybackSessionTimer`, `SessionClock` (new)           | Session timer. Starts on first `LAUNCHING`/`PLAYING`, reset by `PlaylistResetEvent`.                                                                                                                  |
| `event/SkipNextEvent`, `event/player/PlaylistResetEvent` (new)       | Skip request; playlist replaced/cleared (published from `PlaylistModel.clear()`, which covers REPLACE and the clear button).                                                                          |
| `AudioPlayer`                                                        | Skip handler; several progress listeners instead of one; `nextPlaylistItemProperty`; `getVolumeProperty()`, `getCurrentTime()`; null guards in `seek`, the volume listener and the progress listener. |
| `LibraryController` + `library.fxml`                                 | "🎵 Now Playing" button next to play/pause; disabled when stopped/ended.                                                                                                                              |
| `UserPrefKey`, `UserPreferencesService`                              | Keys and methods for the Simple view window bounds (key/value table, no migration needed).                                                                                                            |
| `playback-window.css` (new)                                          | Styles; colors come from `-pw-*` variables set from the palette.                                                                                                                                      |
| Tests (new)                                                          | `SessionClockTest`, `DominantColorTest`, `LyricsFitterTest`.                                                                                                                                          |

### Details worth knowing

- **View = window state.** Maximized view is simply the maximized window, so the title-bar maximize button and the
  in-window button do the same thing.
- **Next** reuses the end-of-track path: the status is set to `ENDED`, then the normal play/pause logic starts the
  already preloaded next track (this is also what makes it start playing when paused).
- **Up next** shows the preloaded track, i.e. exactly what will actually play. If the playlist is edited while a track
  plays, this follows the existing preloading behavior.
- **Closing (X)** hides the playback window and shows the library; it never exits the app. The new window is shown
  before the library is hidden so the app never has zero windows.
- **Lyrics** auto-fit runs at track start, on window open, and when switching to Maximized (the three proposals in
  section 4), only when the checkbox is checked. The checkbox resets to checked on every open.
- **Stopped/ended** with the window open: it stays open on the last track; Next is disabled.
- **Not changed:** the library header, the library waveform colors, the library lyrics tab and its own saved font size.

### Verification status

- Run here: `SessionClock`, `DominantColor`, `LyricsFitter` against the same assertions as the unit tests.
- Compile-checked here: all new UI/logic classes against the real JavaFX API (JavaFX 11, with small stubs for Spring and
  project classes).
- **Not done:** the full Gradle build (Java 25, Spring, jOOQ were unavailable) and any run of the application, so layout
  and timing have not been seen on screen. One pre-existing line (`LibraryController`, unnamed `_` variable) needs Java
  22+, which is expected for this project.

### Manual test checklist

1. Button is disabled at start and when playback ended; enabled while playing and paused.
2. Open: library disappears, Simple view appears with art, titles, waveform filling live, times, controls.
3. Click the waveform to seek (in both views); library waveform behaves as before after returning.
4. Next: disabled on the last track; works while playing and while paused (starts playing).
5. Up next: shows the right track; same artist/album dimmed; "Nothing up next" at the end.
6. Volume slider moves together with the library slider.
7. Session time: empty before first play; keeps running when paused; empty again after replacing or clearing the
   playlist; `h:mm:ss` after an hour.
8. Maximize (button and title bar): lyrics pane, large art, full up-next card. Lyrics fit the pane at track start;
   moving the slider does not change the checkbox; unchecked box keeps the manual size on the next track; "No lyrics"
   message for tracks without lyrics.
9. Colors follow the art and fade between tracks; default colors for tracks without art.
10. Move/resize in Simple view, close, reopen: same position/size; always opens in Simple view; X returns to the
    library; library size/position unchanged.
