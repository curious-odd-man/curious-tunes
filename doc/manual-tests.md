# Manual tests

What to check by hand after the changes from `performance-review.md` (steps 1–6), the spectrum visualiser, its
calibration and the Windows process priority. The automated tests cover the plain-Java parts. Everything below uses
JavaFX, Spring, jOOQ, H2 or Windows itself, and none of it has been run yet.

Each check says what to do and what should happen. Tick it off, or note what happened instead. Sections are in a
sensible order: start-up problems hide everything else.

## 0. Before you start

- [ ] **Back up the database.** Migrations `V006` and `V007` change the schema and can't be undone by the app.
  Copy the H2 file (`~/ctunes/…mv.db`, wherever `spring.datasource.url` points) somewhere safe.
- [ ] **Build and run the automated tests:** `gradle clean build`. Expected: compiles, all tests pass. They are:
  `SpectrumBarsTest`, `SpectrumModelTest`, `ClickTrackTest`, `ProcessPriorityTest`, `AudioFormatTest`,
  `FileStampTest`, `WaveformRasterTest`, `AlacVirtualWavTest`, `JfxPatchVersionTest`, `CoverThumbnailsTest`,
  `JobManagerTest`, plus the existing ones. The first build is the first time much of this code meets a real
  compiler: expect to fix the odd compile error.
- [ ] **Check the hand-edited jOOQ code:** `gradle jooqCodegen`, then `git diff src/main/java/**/dbobj`. Expected:
  the generated `Track`, `TrackRecord`, `Indexes` and `PlaybackHistory` match the hand edits (two new `TRACK`
  columns, one new index). The generator will also add an `AlbumCoverThumb` table and `ALBUM.IMAGE_OVERRIDDEN`.
  That's fine: `AlbumCoverStore` can switch to them later.
- [ ] Run the app **both ways**, since the JavaFX patch loads differently: `gradle run`, and the deployed
  `java -jar cTunes.jar` (`gradle deploy`). Sections 1 and 4 matter for both. The rest can be done once.

## 1. Start-up

- [ ] The app starts, and the log has no new errors or stack traces.
- [ ] Flyway applies `V006__scan_stamps_and_history_index` and `V007__album_cover_thumbnails` once. A second start
  applies nothing.
- [ ] The log says **`Process priority set to Above normal`**. In Task Manager › Details, the cTunes `java.exe` /
  `javaw.exe` shows priority **Above normal**.
- [ ] No `WARNING: A restricted method … has been called` line (native access is enabled in the manifest and the
  run task).
- [ ] With `-Dctunes.priority=high` the log and Task Manager say **High**; with `normal`, **Normal**; with a typo
  such as `fast`, a warning and **Above normal**.
- [ ] Playing the first `.m4a` logs **`ALAC streaming: JavaFX media patch 25.0.1 active`**. If it says *not active*,
  ALAC won't play (there's no fallback any more): note which launch mode it was.

## 2. ALAC playback (option G)

Use a few `.m4a` (ALAC) tracks and a few `.mp3`.

- [ ] An ALAC track starts **right away**, with no noticeable wait, even a long one (10+ min).
- [ ] The duration and the moving playhead are correct from the first second.
- [ ] **Seeking:** click near the end, then near the start, then the middle, several times. Each seek is
  immediate and plays from the right place. No silence, clicks or wrong audio after a seek.
- [ ] **Sound quality:** the track sounds exactly like the same track in iTunes or another player: no noise,
  crackles, distortion or wrong speed.
- [ ] **Next track:** let an ALAC track end with another ALAC track queued. It moves on as before (the next player is
  preloaded). Same for ALAC → MP3 and MP3 → ALAC.
- [ ] Skip quickly through 10 ALAC tracks (Next, Next, …). No errors, and memory doesn't grow steadily (Task
  Manager).
- [ ] If you have a **hi-res (24-bit)** ALAC file: it plays. If it doesn't, note it: JavaFX may not accept 24-bit WAV,
  in which case the virtual WAV would need to deliver 16-bit.
- [ ] MP3 tracks play exactly as before.
- [ ] **Spectrum and waveform** react to ALAC tracks the same as to MP3 (they come from the same player).
- [ ] If you have an **AAC** `.m4a` (an iTunes Store purchase or an AAC import, not ALAC): it plays, from the file.
- [ ] **Patch missing** (optional): start `gradle run` without the two `jfxPatchArgs` in `build.gradle`. If
  `javafx.media` is a named module there, the log has an *ALAC streaming: … not active* error, and playing an
  ALAC track shows the usual playback-error notification instead of hanging.

## 2a. No web server

- [ ] The start-up log has no `Tomcat started on port` line, and nothing listens on the old ports: `netstat -ano |
      findstr :7777` (prod) or `:6666` (dev, apart from the H2 console below) shows nothing.
- [ ] Start-up feels at least as quick as before (it should be about a second quicker).
- [ ] **Dev profile:** the log says `H2 console: http://…:6666`; opening `http://localhost:6666` shows the H2 login.
  Connect with the JDBC URL from the log (`jdbc:h2:file:~/ctunes/ctunes-dev`, user `sa`, password `sa`) while the
  app is running; the tables are there.
- [ ] **Prod profile:** no H2 console line in the log.
- [ ] Lyrics search still finds lyrics. LyricsMode is now on HTTPS and LyricsnMusic is gone; the log shows which
  source answered.
- [ ] The first start after this update applies migration `V008` (log: `Migrating schema "PUBLIC" to version "8 -
      drop pending action"`). There is no `Executing pending actions...` line any more.
- [ ] The tag editor and the lyrics tab still save, and a rescan still reads tags and covers (the tag classes are now
  read-only).

## 2b. After the inspection fixes

- [ ] **History tab:** the table still fills the width and columns can be resized; the last column takes up the slack
  (the resize policy constant changed from the deprecated one).
- [ ] Add albums to the queue 5+ times, then click around the queue: it stays responsive, and in the dev log a
  selection change no longer prints the whole queue several times.
- [ ] The window icons (taskbar, title bar, Alt+Tab) and the preloader icon still show.

## 3. Playback window: spectrum visualiser and calibration

- [ ] Maximized playback window: the visualiser under the album art shows 48 bars (bass left, treble right) with
  peak caps and a fading reflection, as wide as the art.
- [ ] Its colours follow the album art, changing with the background when the track changes.
- [ ] Pause: the bars fall to the floor within about a second and stay there. Play: they move again.
- [ ] The bars move **with** the music, not ahead of it. If they're off, see the calibration below.
- [ ] Resize the window: the visualiser keeps the art's width; the left column still fits at the minimum size.
- [ ] Simple view: no visualiser, nothing broken.
- [ ] **Calibration:** File › *Calibrate visualiser…*, and the visualiser's right-click menu, both open the window.
    - [ ] Music that was playing pauses; it resumes when the window closes. Music that was paused stays paused.
    - [ ] A click every second, every fourth louder; the flash lights up on each click (brighter on the loud one);
      the bars jump.
    - [ ] Moving the slider changes the flash timing at once; −10 / +10 ms buttons work; the value shows in ms.
    - [ ] **Save** keeps the value (reopen to check; restart the app to check it's stored). **Cancel** and the
      window's close button keep the old value. **Reset** puts the slider at 150 ms.
    - [ ] Afterwards the playback window's visualiser uses the saved delay.
    - [ ] No `ctunes-click-track*.wav` files pile up in `%TEMP%`.

## 4. Library scan (steps 1 and 4)

- [ ] **First rescan after the upgrade:** reads every file (as before), and the summary counts look right.
- [ ] **Second rescan, nothing changed:** much faster; nearly everything counted as *unchanged*.
- [ ] Change a track's tags in another program (or iTunes), rescan: that track is *updated* and its new tags show.
- [ ] Tick **Read every file again**: every file is read, as on the first scan.
- [ ] **Stop** during a scan stops it within a second or so; the status says interrupted. A rescan afterwards
  finishes the job.
- [ ] Start a rescan while one is running: nothing breaks (the second is ignored: "already running or queued" in
  the log).
- [ ] Put a `cover.jpg` and a `.txt` in a music folder: ignored, no error. Put a damaged or empty `broken.mp3`:
  the scan finishes, and the summary says *1 could not be read*.
- [ ] A file named `*.MP3` or `*.M4A` (upper case) is picked up.
- [ ] The progress bar moves smoothly, and the UI stays responsive during the scan.
- [ ] A new album gets its cover. Replace the artwork of an album's files (e.g. in iTunes), rescan: the album's cover
  changes in the library, queue and grid.
- [ ] Set an album's cover in the tag editor, then change the artwork in the files and rescan: the tag editor's cover
  is **kept**.
- [ ] Close the app during a scan: it exits within about 5 seconds, and no `java` process is left in Task Manager.

## 5. Library: artists, albums, track rows (steps 2, 5, 6)

- [ ] Click quickly down the artist list (10 artists in a second or two): the album view ends on the last artist
  clicked, never showing a mix or an earlier artist.
- [ ] Opening an artist with many albums is noticeably faster than before.
- [ ] **Covers** in the album list, grid, queue, transport bar and tag editor are sharp (also on a high-DPI screen)
  and correct. Never another album's cover, not even briefly after scrolling.
- [ ] Make the cover size bigger with the slider: covers get sharper, not blurrier.
- [ ] Browse 30+ artists, switching between list and grid: memory (Task Manager) stays moderate and doesn't keep
  climbing.
- [ ] **Grid:** switch to grid; all albums appear as cards, in order. A card's play button, opening an album from a
  card, dragging a card into the queue, and keyboard navigation in the grid work as before.
- [ ] **Track rows** look exactly as before: no-lyrics icon, number, title, duration, spacing, hover colour.
    - [ ] Hover shows a play icon instead of the number; clicking it plays the track. Double-click plays.
    - [ ] Click selects; Ctrl+click toggles; Shift+click selects a range; the selection colour is right.
    - [ ] The track that's playing is highlighted, and the highlight follows playback across albums.
    - [ ] Right-click: *Play now / Play next / Add to queue* act on the selection. *Find lyrics* and *Show in file
      manager* only work with one track; *Edit tags* says "of N tracks" for several; *Copy title and artist*
      copies.
    - [ ] Dragging selected tracks into the queue inserts them where dropped.
    - [ ] Screen reader / accessibility text (if you use one): a row reads title, duration, "playing now", "selected".

## 6. Queue (step 6, 3.1)

- [ ] Rows look exactly as before: border, 2 px gaps, cover, title, artist · album, duration; no list background,
  no stripes, no focus frame around the list.
- [ ] Add a whole big artist or "play all" (1000+ tracks if you can): it appears at once, and scrolling is smooth.
- [ ] Click, Ctrl+click and Shift+click selection; double-click plays; the selection colour is right after scrolling
  away and back.
- [ ] The playing track is highlighted, played ones are marked, a missing file shows its warning icon and tooltip, and
  skipped ones look skipped. Still right after scrolling away and back.
- [ ] Right-click menu: *Play next*, *Move to end*, *Show in library*, *Remove* (Play next / Move to end are disabled
  on the playing track).
- [ ] **Drag to reorder:** the drop guide shows above or below the row under the pointer; dropping puts the track
  there. Same with several selected rows (the drag image shows a count badge).
- [ ] Drag below the last row (into the empty space): the track moves to the end.
- [ ] While dragging, holding the pointer near the top or bottom edge scrolls the list.
- [ ] Drag tracks or an album **from the library** onto a row (inserted there) and onto the empty space (appended).
- [ ] Keyboard: Alt+Up / Alt+Down move the selection, Delete removes it, Ctrl+A selects all. Arrow keys or typing in
  the queue don't break anything.
- [ ] Shuffle, Clear (with its confirmation), and the up/down/delete buttons work.
- [ ] Removing or moving tracks keeps the scroll position; a completely new queue starts at the top.
- [ ] *Scroll to playing* brings the playing track into view. When the next track starts, the list scrolls to it
  only if it was out of view.
- [ ] Narrow the queue pane: the cover disappears below about 230 px; no horizontal scroll bar appears.
- [ ] The queue summary ("N tracks · time") stays right.

## 7. Waveform bar, search, lyrics, history (steps 2 and 5)

- [ ] The waveform bar in the library window fills in as a track plays, played and unplayed colours split at the
  playhead, the hover effect works, and clicking seeks.
- [ ] Paused or minimised: cTunes' CPU use in Task Manager drops to near zero (before, it stayed at a steady
  percentage).
- [ ] Resize the window: the waveform redraws at the new size. The loading animation still shows while a track
  loads.
- [ ] Search: typing shows results; typing fast shows only the results of the final text; clearing the field clears
  them.
- [ ] Lyrics tab: saving edited lyrics shows "Lyrics saved"; finding lyrics online works.
- [ ] History tab: changing the period reloads; "load more" appends; switching periods quickly ends on the last
  one chosen.
- [ ] Playback window: the background palette changes with each track; skipping quickly ends on the right
  track's colours.
- [ ] Debounced settings (window size, sliders) are still saved and restored after a restart.

## When something fails

Note the section and check, what happened, and the log lines around it (with stack traces). Each step's zip
(`curiousTunes-step1.zip` … `step6d.zip`) contains only that step's changes on top of the previous one, so a
problem can be narrowed down by going back a step.
