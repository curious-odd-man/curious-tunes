# Playback Window Mode — Requirements & Design Notes

Status: **DRAFT for review. No code yet.**
Items marked **[PROPOSAL]** are my suggestions and need your confirmation.
Items marked **[CONFIRM]** are my reading of an answer that was ambiguous.

---

## 1. Goal

A separate "now playing" window, available during playback, that shows the current track in a larger, more focused
layout than the library screen. It is inspired by iTunes but is not a copy of it.

## 2. Decisions so far

| #  | Topic                   | Decision                                                                                                                                                                                                                            |
|----|-------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | Relationship to library | Separate window. Library window is hidden while the playback window is open and is brought back when the playback window is left. **[PROPOSAL]** Hide, don't destroy, so library state (selection, tabs, playlist, scroll) is kept. |
| 2  | Views                   | Two views: **Simple** (compact) and **Maximized**. A button switches between them.                                                                                                                                                  |
| 3  | Platform                | Windows only.                                                                                                                                                                                                                       |
| 4  | Frame                   | Normal OS window frame is kept (not frameless).                                                                                                                                                                                     |
| 5  | Waveform                | Same behavior as the library view. Click-to-seek only (no drag-scrubbing).                                                                                                                                                          |
| 6  | Session timer           | Wall clock. Starts at first playback. Resets when the playlist is fully replaced.                                                                                                                                                   |
| 7  | Controls                | Play/pause, next, volume, lyrics text size. No shuffle. No previous.                                                                                                                                                                |
| 8  | Up next                 | Next track in playlist order (there is no shuffle playback). Parts matching the current track (artist, album) are dimmed.                                                                                                           |
| 9  | Lyrics                  | Scrollable, displays stored lyrics only (no fetching). Shows a "no lyrics" message when absent.                                                                                                                                     |
| 10 | Entry button            | On the library view, next to play/pause. Disabled when nothing is playing. No keyboard shortcut.                                                                                                                                    |
| 11 | Persistence             | Window size and position are remembered. Last-used view is **not** remembered.                                                                                                                                                      |
| 12 | Look                    | Colors derived from album art.                                                                                                                                                                                                      |

## 3. Requirements (mapped to the original list)

| #   | Requirement                                       |                       Simple                        | Maximized |
|-----|---------------------------------------------------|:---------------------------------------------------:|:---------:|
| R1  | Title, album, artist                              |                         yes                         |    yes    |
| R2  | Seekable waveform (click to seek)                 |                         yes                         |    yes    |
| R3  | Elapsed time and remaining time                   |                         yes                         |    yes    |
| R4  | Album art, if present (placeholder otherwise)     |                        small                        |   large   |
| R5  | Lyrics, if present                                |                **[PROPOSAL]** hidden                |    yes    |
| R6  | Next track button (disabled if none)              |                         yes                         |    yes    |
| R7  | Time since listening session start                |                         yes                         |    yes    |
| R8  | Up next: title, album, artist, art                | **[PROPOSAL]** one condensed line + small thumbnail | full card |
| R9  | Switch between Simple and Maximized with a button |                         yes                         |    yes    |
| R10 | ~~Frameless~~ — decided: keep frame               |                         n/a                         |    n/a    |
| R11 | Button on library view to open this mode          |                         n/a                         |    n/a    |
| R12 | Play/pause                                        |                         yes                         |    yes    |
| R13 | Volume                                            |                         yes                         |    yes    |
| R14 | Lyrics text size control                          |                         n/a                         |    yes    |

### Simple view **[PROPOSAL]**

Condensed layout for a small window: small album art, title / artist / album in a compact block, waveform, elapsed and
remaining time, session time, play/pause, next, volume, and a one-line "Up next" with a small thumbnail (matching
artist/album dimmed). Lyrics are not shown; switch to Maximized to read them.

### Maximized view **[PROPOSAL]**

Everything: large album art, full metadata, waveform, times, session time, controls, scrollable lyrics pane with
text-size control, and a full "Up next" card (art, title, album, artist; matching parts dimmed).

**[CONFIRM]** "Full screen" from the original list became "Maximized window" in your answer. I read it as the normal
window in its maximized state (title bar and taskbar still visible), not exclusive full screen. The switch button
maximizes or restores the window.

## 4. Behavior details

**Opening and closing**

- Opening the playback window hides the library window; leaving it shows the library again.
- The entry button on the library view is enabled only when a track is loaded (playing or paused). **[CONFIRM]** Does "
  nothing is played" also cover the stopped/ended state at end of playlist?
- Closing the playback window with the title-bar X returns to the library; it must not shut down the app. **[CONFIRM]**
  Is that what you want, or should X exit the application?

**Next button**

- Skips to the next item in the playlist. Disabled when there is no next item.
- **[CONFIRM]** If playback is paused when Next is pressed, should the next track start playing right away? (Suggested:
  yes.)

**Up next**

- Shows the next playlist item (the same one that is already preloaded for gapless transition).
- Artist and album text are dimmed when equal to the current track's. Title is always normal.
- When there is no next track: an empty-state message (e.g. "Nothing up next") and Next is disabled.

**Session timer**

- Starts at first playback and runs as wall-clock time (pauses and seeks do not stop it).
- Resets when the playlist is fully replaced (`PlaylistAddMode.REPLACE`); the timer restarts at the next playback.
- Adding or inserting tracks, or playing a specific track (`PUT_AFTER_CURRENT`), does not reset it.
- **[CONFIRM]** Does clearing the playlist (`PlaylistModel.clear()` on its own) count as a full replacement?
- **[CONFIRM]** What is shown before the first playback after a reset: `0:00` or a dash?
- Display format **[PROPOSAL]**: `m:ss`, switching to `h:mm:ss` after one hour.

**Lyrics**

- Source: lyrics already stored on the track. No fetching from the window.
- Missing lyrics: show a "No lyrics" message.
- **[CONFIRM]** Your answer was "scroll, by default - fit to size". My reading: the pane scrolls when text overflows,
  and the default text size is auto-fitted to the pane, with the size control overriding it. Is that right, or did you
  mean something else?
- **[CONFIRM]** Text size control: stepper (A− / A+) or slider? Remembered between sessions?

**Volume**

- Shares the same value as the library volume slider (changing one moves the other). Saved to preferences the same way
  as the library slider.

**Window state**

- Size and position are saved to user preferences (separate from the library window's).
- The view is always Simple on open. **[CONFIRM]** Does the saved size/position apply to Simple view only, since
  Maximized is a maximized state? (Suggested: yes.)

**Colors from album art**

- Derive a palette (e.g. dominant color) from the album art for the window background, with text color chosen for
  readable contrast.
- No art: fall back to a default dark palette.
- **[CONFIRM]** Should the waveform colors (currently fixed orange/grey) also follow the art palette, or stay as in the
  library?
- **[CONFIRM]** Animate color changes between tracks, or switch instantly?

## 5. Technical findings that shape the design

These come from reading the current code.

1. **Waveform is built live and held in a singleton.** `ProgressCanvasController` is a Spring singleton bound to one
   `Canvas`. It fills the bars from `AudioSpectrumListener` data during playback, and the unheard part of a track is
   empty. It is not stored. A second canvas needs the waveform data separated from rendering (shared waveform model, two
   renderers). Because the library window is hidden while this mode is open, only one canvas is visible at a time, but
   both should read the same data.
2. **`AudioPlayer.linkWithUi(...)` accepts a single progress listener and a single waveform controller.** The playback
   window needs to receive the same updates, so this must become multi-listener (or move to observable properties on a
   shared playback model).
3. **No skip logic exists.** A track advances only when the media ends (`onEndOfMedia` → `PlayPauseEvent` with status
   `ENDED`). Next needs a new event/handler that reuses the already preloaded `nextPlayer`/`nextPlaylistItem`. Edge
   cases: no next item, pressing Next quickly, Next while paused or while still `LAUNCHING`.
4. **Library header state is set inside `LibraryController`'s status listener.** Title, album, artist, times, and lyrics
   are pushed directly into library labels. A shared playback view-model would avoid duplicating this in the new window.
5. **Single `Stage` in `StageManager`.** The playback window will be a second `Stage`. Its close request must not
   publish `UserShutdownApplication` (the primary stage's handler does).
6. **Session timer is new.** `MediaPlayerListeningTracker` only counts listened time per track. The session timer needs
   its own component, driven by playback status and playlist-replace events.
7. **Album art** is `byte[]` on the album record; `noimage.png` exists as the placeholder. Palette extraction should be
   done once per track change, off the UI thread if it is slow.
8. **Preferences** go through `UserPreferencesService` / `UserPrefKey`. New keys are needed for the window's size and
   position (and lyrics size if remembered). Need to check whether a Flyway migration is required.

## 6. Out of scope (for now)

- Frameless window, shuffle, previous track, keyboard shortcuts.
- Lyrics fetching or editing from this window.
- Remembering the last-used view.
- Timed/synced lyrics.
- A Next button on the library view (it is only requested for the new window).

## 7. Suggested implementation order (once the above is confirmed)

1. Shared playback model (current item, next item, status, times) and multi-listener support in `AudioPlayer`.
2. Skip-to-next event and handler.
3. Session timer component.
4. Waveform split into model and renderer.
5. Playback window FXML/controller (Simple layout), then Maximized layout and view switch.
6. Up next panel, lyrics pane, text size control.
7. Album-art palette.
8. Library button, hide/show handoff, window state persistence.

## 8. Open questions (summary)

1. Does X on the playback window return to the library, or exit the app?
> return to library
2. Is "Maximized" the normal maximized window state (not exclusive full screen)?
> yes
3. Is the Simple view content proposal acceptable (no lyrics, one-line Up next)?
> yes
4. Next while paused: start the next track immediately?
> yes
5. Does clearing the playlist alone reset the session timer? What is shown before first playback?
> yes. nothing is shown
6. Lyrics: confirm the "scroll + auto-fit default" reading; text size control type and persistence.
> correct
7. Entry button: also disabled when stopped/ended with a loaded track?
> yes
8. Do waveform colors follow the album art palette? Animate color transitions?
> yes and yes
9. Does saved window size/position apply to Simple view only?
> yes
