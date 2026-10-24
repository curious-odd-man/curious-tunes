# Playback Window Mode — Requirements & Design Notes

Status: **DRAFT v2 — all answers from review round 1 incorporated. No code yet.**
Items marked **[PROPOSAL]** are my suggestions that have not been explicitly confirmed.
Items marked **[OPEN]** are still unanswered.

---

## 1. Goal

A separate "now playing" window, available during playback, that shows the current track in a larger, more focused
layout than the library screen. It is inspired by iTunes but is not a copy of it.

## 2. Decisions

| #  | Topic                   | Decision                                                                                                                                                                                                                                     |
|----|-------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | Relationship to library | Separate window. The library window is hidden while the playback window is open and is brought back when the playback window is left. **[PROPOSAL]** Hide rather than destroy, so library state (selection, tabs, playlist, scroll) is kept. |
| 2  | Closing                 | The X button on the playback window returns to the library. It does not exit the app.                                                                                                                                                        |
| 3  | Views                   | Two views: **Simple** (compact) and **Maximized**. A button switches between them.                                                                                                                                                           |
| 4  | Maximized               | The normal maximized window state (title bar and taskbar visible), not exclusive full screen.                                                                                                                                                |
| 5  | Platform                | Windows only.                                                                                                                                                                                                                                |
| 6  | Frame                   | Normal OS window frame is kept (not frameless).                                                                                                                                                                                              |
| 7  | Waveform                | Same behavior as the library view. Click-to-seek only.                                                                                                                                                                                       |
| 8  | Session timer           | Wall clock. Starts at first playback. Resets when the playlist is fully replaced **or cleared**. Shows nothing until the first playback after a reset.                                                                                       |
| 9  | Controls                | Play/pause, next, volume, lyrics text size. No shuffle, no previous.                                                                                                                                                                         |
| 10 | Next while paused       | Starts playing the next track immediately.                                                                                                                                                                                                   |
| 11 | Up next                 | Next track in playlist order. Artist/album that match the current track are dimmed.                                                                                                                                                          |
| 12 | Lyrics                  | Scrolls when text overflows. Default text size auto-fits the pane; the size control overrides it. Shows stored lyrics only (no fetching). Shows a "no lyrics" message when absent.                                                           |
| 13 | Simple view content     | No lyrics, one-line "Up next" with small thumbnail.                                                                                                                                                                                          |
| 14 | Entry button            | On the library view, next to play/pause. Enabled only while a track is playing or paused; **disabled when stopped or ended**. No keyboard shortcut.                                                                                          |
| 15 | Persistence             | Window size and position are remembered, **for Simple view only**. Last-used view is not remembered (always opens in Simple).                                                                                                                |
| 16 | Look                    | Colors derived from album art, including the waveform colors. Color changes between tracks are animated.                                                                                                                                     |

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
| R14 | Lyrics text size control                                         |            n/a             |    yes    |

## 4. Behavior details

**Opening and closing**

- Opening hides the library window; leaving (X button) shows it again. Playback continues uninterrupted either way.
- The playback window always opens in Simple view.
- If playback becomes stopped or ended while the window is open, the window stays open showing the last track. *
  *[PROPOSAL]** Next is disabled and play/pause restarts or resumes as in the library. Tell me if you would rather it
  close automatically.

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
- Display format **[PROPOSAL]**: `m:ss`, switching to `h:mm:ss` after one hour.

**Lyrics**

- Source: lyrics already stored on the track. No fetching.
- Missing lyrics: a "No lyrics" message.
- Text size: auto-fitted to the pane by default; the control sets an explicit size.
- **[OPEN]** Control type: stepper (A− / A+) or slider?
- **[OPEN]** Is the chosen size remembered between sessions?

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
- **[PROPOSAL]** The library's waveform keeps its current fixed colors; only the playback window recolors.

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

## 8. Remaining open questions

1. Lyrics text size control: stepper or slider?
> slider
2. Is the chosen lyrics size remembered between sessions?
> no, remember - first fit lyrics. btw need a checkbox to disable this.
3. Session timer format: `m:ss` then `h:mm:ss` after an hour — OK?
> ok
4. When playback stops or ends with the window open: stay open (my proposal) or close automatically?
> stay
5. Should the library waveform keep its fixed colors (my proposal), or also follow the album art?
> keep
6. Hide (my proposal) vs. destroy the library window while the playback window is open — OK to hide?
> hide
