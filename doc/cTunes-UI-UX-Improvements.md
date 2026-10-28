# cTunes — UI/UX Improvement Suggestions

**Scope:** styling and convenience improvements for the JavaFX UI (`src/main/resources/fxml/*`, `styles/*.css`, `ui/**`
controllers, `PlaybackWindow`).
**Status:** the original review (sections 1–15) was written before any code changed. Since then the quick wins (except
#7), the medium items and three of the four large items have been implemented (the light/dark theme was skipped on
purpose). **Section 16 at the end lists the steps that are still open** and is the part to review; sections 1–15
describe the original state of the code.
**How to read this:** items are grouped by area, each tagged with an effort estimate (**S** ≈ under an hour, **M** ≈
half a day, **L** ≈ a day or more). Section 1 is a prioritized shortlist; the rest is the detail behind it.

---

## 0. What's already good

Worth keeping as the reference standard for the rest of the app:

- The **Playback window** is the most polished part: album-art-derived palette with an animated transition, auto-fit
  lyrics, a dimmed "same artist/album as current" treatment in Up Next, remembered bounds with a disconnected-monitor
  check.
- Window size, split-pane dividers and volume are persisted.
- Lyrics-missing warning on tracks, the orange "overridden field" marker in tag editing, and the play overlay on album
  art are good ideas that just need finishing (see below).

---

## 1. Prioritized shortlist

### Quick wins (mostly S)

| #  | Change                                                                                                                                                    | Where                                                                            | Effort |
|----|-----------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------|--------|
| 1  | Add **tooltips** to every icon-only button (artist row, playlist toolbar, transport, playback window)                                                     | `library-artist.fxml`, `library-playlist.fxml`, `library.fxml`, `PlaybackWindow` | S      |
| 2  | **Keyboard shortcuts**: Space = play/pause, Enter = play selected, Delete = remove from queue, Ctrl+F = search, Esc = leave Now Playing                   | none exist today                                                                 | S–M    |
| 3  | Fix **selected vs. playing** row collision in the queue (selection is invisible on the playing row)                                                       | `global.css`                                                                     | S      |
| 4  | Add **hover / focus / pressed** states to list rows and buttons (currently none in library)                                                               | `global.css`                                                                     | S      |
| 5  | Replace pure-blue `#00F` selection with a softer accent + inactive-window variant                                                                         | `global.css`                                                                     | S      |
| 6  | Show **"No track number"/"—"** instead of the literal text `null`; show `--:--` instead of `-1` for unknown duration                                      | `LibraryArtistAlbumTrackController`, `TimeUtils`                                 | S      |
| 7  | History tab: drop fractional seconds from the time column (`LocalTime.toString()`), format dates for the user's locale                                    | `LibraryHistoryTabController`                                                    | S      |
| 8  | Lyrics tab: enable word-wrap, remove the dead 120 px right gutter, disable Save until edited                                                              | `library-tab-lyrics.fxml`                                                        | S      |
| 9  | Remove or hide **dead menu items / buttons** (File › Close, Edit › Delete, Help › About, "Update DB from FIle", queue context menu items) or wire them up | `library.fxml`, `library-tab-edit-tags.fxml`, `PlaylistItemController`           | S      |
| 10 | Rescan dialog: remember last folder instead of hard-coded `D:\iTunes\…`, make Enter = Scan, handle Cancel in the folder picker                            | `LibraryController`, `RescanLibraryController`                                   | S      |

### Medium (M)

- A real **status bar / toast** area for background work and errors (scan progress, "lyrics not found", "tags saved").
- **Search/filter** for the artist list (a `searchField` is already declared in `LibraryController` but never added to
  the FXML).
- **Queue improvements**: header with count/total time, scroll-to-playing, confirm/undo for "Clear", drop-at-end and
  above/below drop indicator.
- **Library track list**: context menu with Play / Play next / Add to queue, "now playing" marker, multi-select.
- **Album hero art/grid**: make the 350 px cover layout adjustable (grid ⇄ list).
- Extract **design tokens** (colors, radii, font sizes) into `global.css` so the library and Now Playing windows look
  like one app.

### Larger (L)

- Virtualize the artist list (`ListView` with a cell factory) and add an A–Z jump.
- Light/dark theme following the OS setting.
- Restructure the top area of the library window into a conventional bottom **transport bar**.
- Lyrics inside the simple-view Now Playing window; fullscreen mode.

---

## 2. Global styling and theming

**Observations**

- `global.css` is ~35 lines of ad-hoc rules. Colors are hard-coded in at least five places: CSS (`#00F`, `#5AA`,
  `rgb(204,190,255)`, `rgb(165,165,165)`), FXML (`textFill="#2200ff"`, `textFill="RED"`, `#00000080`), Java (
  `ProgressCanvasController` orange/grey, `markIfDifferent` inline orange border, `parentPane.setStyle(...)`), and
  `PlaybackPalette`.
- The library window is the default light Modena theme; the Now Playing window is a dark gradient. Switching between
  them (the library window is *hidden* while Now Playing is open) is a visual jolt.
- Fonts are inconsistent: `Consolas Bold` (Windows-only, silently falls back elsewhere), `System Bold`, plain system
  font, with sizes 12/14/16/17/18/24 chosen per node in FXML.
- Emoji (`▶ ⏸ ⏭ 🔀 ➕ ❌ ⬆ ⬇ 💿 🎵 🔊 📂 💾 🔁 🔎`) are used as icons. They render differently per OS/font, can't be recolored by
  CSS, and some are ambiguous (`➖` vs `❌` in the queue).
- Only the primary Scene gets `global.css`. The Rescan dialog and the Now Playing Scene don't, so they can't share
  tokens.

**Suggestions**

1. **Define tokens once** as looked-up colors on `.root` and use them everywhere (S–M). Illustrative only:
   ```css
   .root {
       -ct-accent: #3b82f6;
       -ct-accent-soft: derive(-ct-accent, 80%);
       -ct-playing: #1f9d8f;
       -ct-border: rgba(0,0,0,0.12);
       -ct-text-dim: rgba(0,0,0,0.55);
       -ct-danger: #d64545;
   }
   ```
   Then replace the hex literals in FXML/Java with style classes (`.danger`, `.dim`, `.accent`) so nothing is styled
   inline. Inline `setStyle` in `markIfDifferent` should become a `.field-overridden` class.
2. **Replace emoji with an icon font/SVG** (Ikonli, or SVG paths in `-fx-shape`) (M). Gives consistent size, color and
   hover behavior, and lets destructive actions be red via CSS instead of per-button `textFill`.
3. **Pick one UI font and a type scale** (e.g. 12 / 14 / 16 / 24) as CSS classes (`.text-small`, `.text-body`,
   `.text-title`) rather than `<Font size=…>` in each FXML node (S–M). Use a monospace face only where alignment
   matters (times), with a cross-platform fallback.
4. **Dark theme / follow OS** (L). Since the Now Playing window is already dark, consider a dark variant of the
   library (override Modena base colors via looked-up colors, or adopt a theme library such as AtlantaFX). JavaFX can
   read the OS color scheme through `Platform.getPreferences()`.
5. **Load the stylesheet on every Scene** via a small helper so the dialog and the Now Playing window pick up tokens
   too (S). Use `getResource(...).toExternalForm()` rather than the relative string `"styles/global.css"` in
   `StageManager`.
6. **Window-inactive state**: add `:focused`-aware selection color so the selected row greys out when the window loses
   focus (S).
7. **Minimum window size** for the library stage. FXML prefers 1920×1080 and nothing sets a minimum, so on a small
   display or when squeezed the three-pane split collapses (S).

---

## 3. Library window — header / transport area (`library.fxml`, `LibraryController`)

**Observations**

- The top area stacks three rows: transport + volume, a "Title – Album – Artist" line flanked by two 24 pt time labels,
  and a 100 px-tall waveform. It takes a lot of vertical space and mixes concerns.
- The track line is three `Label`s joined by literal `-` labels in `Consolas Bold 17`. They don't ellipsize or share
  width fairly, so long titles will push things off-screen. There is also a stray `layoutX/layoutY` on one separator.
- The 50×50 cover is not clickable.
- Play/Pause button text is `" ▶ "` (padded with spaces) and there is **no Previous/Next/Stop** in the library window (
  Next exists only in Now Playing).
- The volume slider shows tick marks *and* tick labels, which is visually heavy for a simple 0–100 control; there's no
  mute and no icon. Volume is saved only on `mouseReleased`, so keyboard changes (arrow keys on the slider) aren't
  persisted.
- On `LAUNCHING`, the remaining time is set to the **raw duration number** (`'-' + playlistItem.getDuration()`) and
  elapsed to `"0"`, instead of the formatted `mm:ss` used a moment later, so the labels visibly change format.
- **Scan progress hijacks the now-playing widgets.** `onBackgroundProcessEvent` writes process name/description into the
  track labels, the raw counters into the time labels, and drives the waveform canvas as a progress bar. It also sets
  `currentTrackName` twice (second call sets it to `""`), so the process name is immediately erased. If music is playing
  during a scan, the player display is corrupted.
- No UI ever publishes `InterruptBackgroundProcessEvent`, so a scan can't be cancelled from the UI even though the
  backend supports it.
- The idle state (nothing played yet, or after `ENDED`) shows blank labels and a flat canvas, with no "Nothing playing"
  message.

**Suggestions**

1. **Move to a conventional transport bar** (L): `[cover] Title / Artist — Album` on the left, `⏮ ⏯ ⏭` + seek bar in the
   center, volume + "Now Playing" on the right. Frees ~60 px of height and matches user expectations from other players.
   Reduce the waveform to ~56–64 px (the Now Playing window already uses 56).
2. Make the track text a **two-line block** (title bold, "Artist — Album" dim) with `ELLIPSIS` and tooltips with the
   full text (S).
3. **Click the cover or title → open Now Playing**; click artist/album → jump to that artist/album in the library (S–M).
4. Add **Prev / Next** (and optionally Stop) buttons, remove the padded-space text, and set tooltips including the
   shortcut (S).
5. **Simplify the volume control**: drop tick labels, add a speaker icon that toggles mute and changes with level, show
   the value in a tooltip while dragging, and persist on `valueChanging == false` as well as mouse release (S–M).
6. **Separate background-task UI** (M): a slim status bar at the bottom with a `ProgressBar`, a one-line description ("
   Scanning library… 1,240 / 5,000"), and a Cancel button wired to `InterruptBackgroundProcessEvent`. Stop writing into
   the player labels/canvas. On completion show a short summary ("Added 312 tracks, 4 updated").
7. Always format times through `TimeUtils` — including the `LAUNCHING` branch (S).
8. Add an **idle/empty state**: "Nothing playing — double-click a track to start" (S).
9. **Seek bar** (`ProgressCanvasController`, `onProgressClicked`): add a hand cursor, a visible playhead line, and a
   hover time-preview tooltip (M). Guard the click handler when nothing is playing (today it chains
   `getCurrentlyPlayingItem().get()` with no check). Seeking before the waveform exists should still work.
10. Make the seek-bar colors come from the same tokens/palette as Now Playing instead of the separate hard-coded
    orange/grey (S).

---

## 4. Artist list (left pane — `library-artist.fxml`, `LibraryArtistController`)

**Observations**

- Every artist row always shows a **2×2 grid of four buttons** (`▶`, `▶➕`, `🔀`, `🔀➕`). That's constant visual noise in a
  long list, and the glyph combos are cryptic.
- The row image is always `noimage.png` (the artist-image code is a `FIXME`), so every row carries the same placeholder.
- No hover state, no hand cursor; selection is solid `#00F` with white text.
- Rows are loaded **one FXML per artist, one `runOnFxThread` per artist**, into a plain `VBox` inside a `ScrollPane`.
  For a large library this will be slow to fill and heavy in memory, and the UI updates incrementally while the user is
  already interacting.
- No search/filter, no alphabetical grouping, no sort options. There's no persisted selection, so after a restart the
  right side is empty.
- Clicking an already-selected artist re-triggers a full reload of the albums view.

**Suggestions**

1. **Reveal row actions on hover/selection** (S): hide the button grid until `:hover` or selected, and make it a single
   row of icon buttons with tooltips: *Play*, *Add to queue*, *Shuffle*, *Add shuffled*. Alternatively, one "▶" button
   plus a "⋯" menu.
2. Replace the placeholder with a **generated avatar** (circle with initials, color derived from the name hash) until
   real artist images exist (S–M). It also gives rows visual rhythm.
3. **Search box** at the top of the pane with Ctrl+F focus and live filtering; include a clear (×) button and "No
   artists match" empty state (M). Extend later to albums/tracks.
4. **A–Z index / sticky letter headers** and a sort toggle (A–Z, Recently played, Most played — the history table
   already has the data) (M).
5. **Virtualize** with `ListView<ArtistRecord>` and a cell factory (L). Makes selection, keyboard navigation (↑/↓,
   type-ahead) and hover states come "for free" and fixes load jank.
6. **Remember the last selected artist** and scroll position (S–M).
7. Ignore clicks on the already-selected artist; show a loading state while albums load (see next section).
8. **Double-click an artist to play** (replace queue), matching the track behavior (S).
9. First-run **empty state**: when the library is empty, show "No music yet — Add a music folder…" with a button that
   opens the Rescan dialog (S).

---

## 5. Albums tab and track list (`library-artist-album*.fxml`, `library-album-track.fxml`)

**Observations**

- Each album uses a **fixed 350×350 cover** beside the track list, so every album is ≥350 px tall. A 20-album artist is
  a very long scroll, and there is no overview.
- The play overlay on the cover is **hover-only** (opacity 0 → 1), hard to discover, unreachable by keyboard, and for
  albums it offers **only "replace queue and play"** — the artist row has four modes, albums have one.
  `fadePlayImageButtonTo` creates a new `FadeTransition` on every enter/exit without stopping the previous one, which
  can flicker when moving the mouse quickly.
- `albumDetails` shows just the year (or `🎵` when unknown). Track count, total duration and genre aren't shown even
  though the data is available.
- When switching artists, albums are **loaded on a background thread and added all at once at the end**. Until then the
  pane shows only the artist title — no spinner. If the user clicks a second artist quickly, both threads append their
  albums, so albums from two artists can end up mixed in one view (the earlier thread isn't cancelled/ignored).
- Tracks: single-click only selects; **double-click** plays. There's no visible affordance for that. There's no "now
  playing" marker for the track that's currently playing.
- Selected track is solid `#00F`; rows have 1 px top/bottom borders but no hover state.
- Track number prints `null` when missing (`String.valueOf(Integer)`); duration label has no alignment so times don't
  line up in a column.
- The red `⚠` ("No lyrics") on every track without lyrics is visually loud — for a library with many un-lyric'd tracks
  the list will look like an error screen.
- Right-click selects and opens a context menu with only **"Edit Tags"**.
- Track list splits into two columns when a disc has >10 tracks. Reading order (down the left, then down the right) is
  fine, but the two columns have independent row heights, so rows can misalign.
- Disc headers use `💿 N` on a mid-grey bar; the tag editor spells it "Disk Number" (inconsistent: "disc" vs "disk").

**Suggestions**

1. **Album layout toggle: Grid ⇄ List** with a cover-size slider (M–L). Grid of ~160–200 px covers gives an overview;
   clicking a cover expands/shows the track list. For the list layout, default to a smaller cover (~200 px).
2. **Always-visible, keyboard-accessible album actions** (S–M): a small toolbar under the title — *Play*, *Add to
   queue*, *Shuffle* — replacing the hover-only overlay (the overlay can stay as a bonus). Stop the previous
   `FadeTransition` before starting a new one.
3. **Richer album subtitle**: `2019 · 12 tracks · 48:12 · Genre` (S).
4. **Loading and race handling** (M): show a spinner/skeleton in the albums pane; ignore results from superseded
   requests (compare a request token before adding to the view); add albums progressively in year order so the first
   ones appear quickly.
5. **Album sort control** (year ↑/↓, name) with the choice remembered (S).
6. **Track row polish** (S–M):
    - Hover highlight, and on hover replace the track number with a small ▶ (a familiar pattern that also advertises
      double-click).
    - Show a "now playing" indicator (animated bars or ▶ in accent color) on the row whose track is currently playing.
    - Right-align a fixed-width duration column.
    - Show `—` for missing track numbers.
    - Replace the red ⚠ with a dim "♪̸"/"no lyrics" icon that only reads as a hint (or show it only on hover and offer "
      Find lyrics" in the context menu).
7. **Context menu** on tracks: *Play now*, *Play next*, *Add to queue*, *Edit tags*, *Find lyrics*, *Show file in
   folder*, *Copy title/artist* (M). Right-click should not clear an existing multi-selection.
8. **Multi-select** in the library track list (Ctrl/Shift like the queue already does) with Enter/Add-to-queue acting on
   all selected (M).
9. **Keyboard**: ↑/↓ moves between tracks, Enter plays, Shift+Enter adds to queue (S–M).
10. **Drag tracks/albums into the queue** (M–L). The queue already supports internal drag-and-drop; accept drops from
    the library too.
11. Unify wording: "Disc" everywhere (S).

---

## 6. Queue / playlist (right pane — `library-playlist.fxml`, `playlist-item.fxml`, `PlaylistItemController`)

**Observations**

- Toolbar: `⬆ ⬇ ➖ 🔀 ❌` with blue/red `textFill`, no labels, no tooltips. `➖` (remove selected) and `❌` (clear all) look
  like synonyms. **"Clear" is one click, no confirmation, no undo.**
- Context menu has *Remove / Move to end / Move to next*, but all three are **no-ops** (`// FIXME - implement actions`).
- The queue is **rebuilt from scratch** (`playlistNodes.clear()` + re-add) on every add, shuffle and drag-drop. Result:
  flicker, lost scroll position, and lost selection visuals.
- No header: no track count, no total remaining time, no "Now playing" shortcut. The view doesn't scroll to the
  currently playing item when playback advances.
- Empty queue is a blank pane.
- Status styling: `PLAYING` → teal `#5AA`, `PLAYED` → 50% opacity (including the cover), but `FILE_NOT_FOUND`,
  `SKIPPED`, `TO_BE_SKIPPED` map to **no styling at all**, so missing files look identical to normal items.
- **Selection vs. playing**: both use a full background-color class; `.playlist-item-playing` is declared after
  `.selected-item`, so a row that is both selected and playing shows no selection.
- Subtitle uses `#00000080` (50% black) and the separator `"  ---  "` between artist and album; on the teal playing row,
  contrast should be checked.
- Drag-and-drop shows a 2 px line on the **top** of the target only, always inserting *before* it, so you can't drop at
  the very end; no auto-scroll while dragging near the edges; only one row is dragged even when several are selected.
- The vertical scrollbar policy is `ALWAYS`, taking permanent width in a narrow pane (the pane is only ~20% wide at the
  default divider positions).
- The current-playing row is not visually distinct enough at a glance (no icon).

**Suggestions**

1. **Labeled/tooltipped toolbar** with grouped actions (S): *Move up/down*, *Remove*, *Shuffle*, *Clear*. Make *Clear*
   use a neutral style, and show a **confirmation or an "Undo" snackbar** (M).
2. **Queue header**: `Queue · 24 tracks · 1:32:10` with a "↓ Now playing" button that scrolls to the current row (S–M).
   Also auto-scroll to the playing row on track change unless the user has scrolled manually recently.
3. **Implement the context menu** (*Remove*, *Play next*, *Move to end*, *Play now*, *Show in library*) or remove the
   unfinished items (S for removal, M for implementing).
4. **Update incrementally** instead of rebuilding: append new rows on add, swap on move (the up/down already does), and
   only rebuild on shuffle (M). Preserve scroll position when you do rebuild.
5. **Separate the visuals for selected and playing** (S): e.g. playing = accent-colored left bar + ▶ icon + bold title;
   selected = background tint. Both can then apply at once. Add a style for `FILE_NOT_FOUND` (strikethrough + warning
   icon + tooltip "File not found at …") and for `SKIPPED`.
6. **Improve drag-and-drop** (M): show an insertion line above *or* below depending on pointer half; allow dropping at
   the end (drop on the empty area of the list); auto-scroll near edges; drag all selected rows with a count badge in
   the drag image.
7. **Empty state**: "Queue is empty — double-click a track, or press ▶ on an artist or album" (S).
8. **Narrow-pane layout**: let the scrollbar overlay or auto-hide (`vbarPolicy=AS_NEEDED`), and consider hiding the
   cover when the pane is very narrow (S–M).
9. Replace the `"  ---  "` separator with ` · ` or `—` and use a dim style class (S).
10. **Keyboard**: Delete removes selected, Alt+↑/↓ moves, Ctrl+A selects all, Enter plays selected (S–M).
11. Persist the queue across restarts (L). I didn't find a queue table in the migrations (`pending_action`,
    `playback_history`, `track_overrides_history`, `user_preferences`), so this may not be saved today — worth
    confirming.

---

## 7. Lyrics tab (`library-tab-lyrics.fxml`, `LibraryLyricsTabController`)

**Observations**

- `TextArea` has no `wrapText`, so long lines scroll horizontally; the right anchor of **120 px** leaves an empty dead
  strip on the right.
- Reading mode looks like an editor (grey bordered `TextArea`, scrollbars) — it is a *display* surface 95% of the time.
- **Save is enabled from the start** (nothing disables it initially); with no track loaded, `playlistItem` is null and
  clicking Save will throw. `Enable Edit` sets the area editable even when the toggle is being turned *off*.
- No unsaved-changes indicator; switching track while editing silently discards edits.
- "Find lyrics" gives **no feedback**: no spinner while searching and, on failure, only `log.warn(...)`.
- The font-size slider sits in the button bar with tick labels; it's saved on `mouseReleased` only.
- Tab label "Current Lyrics" doesn't make clear that it follows playback, and the tab has no empty state when nothing is
  playing.

**Suggestions**

1. **Wrap text on**, remove the 120 px gutter, add padding and a comfortable default size/line height (S).
2. **Read mode vs. edit mode** (M): show lyrics in a clean, borderless, centered (or left-aligned) scrolling view; only
   reveal the editor (with border + toolbar) when "Edit" is on.
3. **State handling** (S): Save disabled until the text differs from the original; "Edit" toggle controls editability
   both ways; a small "● Unsaved" label; a "Revert" button.
4. **Feedback for Find lyrics** (S–M): disabled button + inline spinner while searching; a non-blocking message ("No
   lyrics found on DarkLyrics / LyricsMode / …") on failure; show which source succeeded.
5. **Empty states**: "Nothing is playing" and "No lyrics for this track — Find lyrics" with the button inline (S).
6. Make font size a compact **A− / A+** control (or keep the slider without tick labels), saved on
   `valueChanging == false` (S).
7. Share lyric size/auto-fit preferences with the Now Playing window instead of two unrelated settings (S–M).

---

## 8. Edit Tags tab (`library-tab-edit-tags.fxml`, `LibraryTagEditTabController`)

**Observations**

- The tab is **empty and unexplained** until you pick *Edit Tags* from a track's context menu. Nothing says which track
  is being edited once you're in it.
- Toolbar: `💾 Save`, `🔁 Update DB from FIle` (typo; **logs "Not implemented"** when clicked), `🔎 Find Lyrics`. Save has
  **no success/failure feedback**, no dirty state, no "revert".
- 9 rows of fields in a `GridPane` inside an `HBox`, **not in a ScrollPane**, so on shorter windows the lower fields (
  Genre, Composer, Cover) get clipped.
- Numeric fields (`Track Number`, `Disk Number`) are plain text fields; invalid input is only discovered at conversion
  time. No placeholder/prompt text, no date format hint for *Release Date*.
- **Overridden fields** are marked with an orange 2 px border set inline, but nothing explains what orange means, and
  there's no way to restore the original value even though the override history table exists.
- The cover is a 100×100 image with no placeholder, no "change cover" action.
- "Disk Number" vs "Disc" (see §5). Lyrics are edited both here and on the Lyrics tab, with no sync hint.

**Suggestions**

1. **Context header** at the top: cover thumbnail + "Editing: *Title* — Artist / Album" and the file path (dim,
   selectable, tooltip) (S).
2. **Empty state**: "Select a track and choose *Edit Tags* (right-click)" (S).
3. **Dirty tracking** (M): highlight changed fields, enable Save only when something changed, show "Reset" and "Revert
   all". Ctrl+S saves; Enter in a field moves to the next one.
4. **Save feedback** (S): inline "Saved ✓" or a toast; show errors visibly.
5. **Explain and act on overrides** (M): add a small "↺" button inside overridden fields (tooltip: "Edited — original
   value: …; click to restore") and a legend line; replace the inline orange style with a CSS class.
6. Wrap the form in a **`ScrollPane`**, and give labels a consistent width and right alignment (S).
7. **Validation** (S–M): numeric fields use a text formatter (digits only), invalid state with red border + tooltip;
   placeholder `YYYY` or `YYYY-MM-DD` in Release Date.
8. Remove or hide *Update DB from File* until implemented; fix the label typo (S).
9. Let users **change the cover** (choose file / drop an image / paste), and show a placeholder when there is none (
   M–L).
10. Consider a **batch edit** mode when several tracks are selected (e.g. fix an album name for the whole album) (L).

---

## 9. History tab (`library-tab-history.fxml`, `LibraryHistoryTabController`)

**Observations**

- The four stats ("Total time listened", "Songs played", …) are plain bold label/value pairs at the top-left in an
  `AnchorPane → GridPane` with nine column constraints (most unused) — the layout is a leftover.
- Table: date and time are separate columns; `LocalTime.toString()` shows fractional seconds (e.g. `14:03:22.481932`);
  dates are ISO. Columns have equal default widths, aren't resizable-friendly for long titles, and `sortable=false`.
- Rows can't be acted on (no double-click to play, no context menu).
- `renewStats()` runs **on the FX thread**, fetches all history, then calls `getAlbum` and `getArtist` **once per row
  ** (N+1 queries). The `.limit(100)` is commented out with a TODO. Each time the tab is selected the UI can freeze,
  increasingly as history grows.
- No empty state, no loading indicator, no filtering by date or artist.

**Suggestions**

1. **Stat cards** across the top (S–M): large number + small caption — "42 h listened", "1,204 songs", "86 albums", "31
   artists". Optionally "Top artist this month" and a small listening-per-day bar chart (L).
2. **Single "Played at" column** formatted `Today 14:03` / `Yesterday 21:15` / `12 Oct 2026`; hide seconds; show full
   date-time in a tooltip (S).
3. Sensible default column widths (Song and Artist widest), allow sorting, and make the table fill the pane (S).
4. **Row actions** (M): double-click to play, context menu *Play / Add to queue / Show in library*.
5. **Load off the FX thread**, with a spinner, and **page/limit** the table (e.g. latest 200 + "Load more") (M). Join
   albums/artists in one query to remove the N+1 pattern.
6. **Empty state**: "Nothing played yet" (S). Add simple filters (today / 7 days / 30 days / all) (M).

---

## 10. Rescan dialog (`rescan-modal.fxml`, `RescanLibraryController`)

**Observations**

- Absolutely positioned (`layoutX/layoutY`), fixed 600×167: doesn't resize or scale with fonts/DPI. Text field is 51 px
  tall next to a 24 pt `📂` button.
- Default path is hard-coded to `D:\iTunes\iTunes 1\iTunes Media\Music` in `LibraryController.onRescanMenuClicked`.
- *Scan* isn't the default button, so Enter does nothing. No validation that the folder exists.
- If the folder picker is cancelled, `result` is null and `result.getAbsolutePath()` throws.
- After clicking Scan the dialog closes instantly and progress is shown only through the (hijacked) now-playing widgets;
  no completion message, no cancel.

**Suggestions**

1. Use a `GridPane`/`HBox` layout with proper padding; `Label "Music folder"`, `TextField` (grows), `Browse…` button (
   S).
2. **Remember the last folder** in user preferences (and offer a "Recent folders" dropdown) (S).
3. `Scan` as default button, `Cancel` as cancel button, disable Scan when the path is empty or doesn't exist, with
   inline message (S).
4. Null-check the DirectoryChooser result (S).
5. Hook into the status bar from §3.6 for progress, cancel and a completion summary (M).
6. Consider a **Settings** dialog (library folders, default shuffle behavior, theme, lyrics font) instead of
   single-purpose dialogs (L).

---

## 11. Now Playing window (`PlaybackWindow`, `playback-window.css`, `PlaybackWaveformView`)

This window is in good shape, so the suggestions are about discoverability and convenience.

**Observations**

- **Closing the window returns to the library** (the close request is consumed and the library is re-shown). That's a
  deliberate model, but it is unannounced: users clicking ✕ may expect the app to quit or minimize, and there is no
  explicit "Back to library" control.
- The compact/"maximized" layout is chosen by the **OS maximized state**, not by available size. A large, non-maximized
  window still shows the compact layout with no lyrics; a window the user maximizes only to fill the screen is forced
  into the other layout. The toggle button says "Maximize" / "Simple view", which mixes two concepts (window state vs.
  layout).
- **Lyrics exist only in the maximized layout.**
- Buttons use `setFocusTraversable(false)`: they can't be reached or activated from the keyboard, and there are no
  `:focused`/`:pressed` styles. No shortcuts (Space, ←/→, Esc, F11).
- Transport has Play/Pause and Next only: no Previous, no ±10 s seek.
- Icon-only buttons (`▶ ⏸ ⏭`) have no tooltips. The speaker icon is static (not a mute button, doesn't reflect level).
  The volume slider only styles the track/thumb (no filled portion) and shows no value.
- "Session 1:23:45" label has no explanation.
- The waveform: no hover time-preview, no playhead marker; the unplayed part of a not-yet-listened track looks
  flat/empty because the waveform fills in *as you listen* — which can look like a bug the first time.
- The lyrics size and the auto-fit checkbox are **reset on every open** (`autoFitCheck.setSelected(true)` in `open()`),
  the lyrics size slider isn't persisted, and it's separate from the library lyrics font size.
- Window title is always "Now Playing", so the taskbar/Alt-Tab shows no track info.
- `textDim` is the light text at 50% alpha; at 11 px bold ("UP NEXT", "SIZE") that likely falls under 4.5:1 contrast on
  mid-tone album-art backgrounds.

**Suggestions**

1. **Add a visible "◀ Library" button** (with tooltip "Back to library — closing this window does the same") and Esc as
   shortcut (S).
2. **Choose the layout by size** (e.g. width ≥ 1000 px and height ≥ 600 px ⇒ wide layout), independent of maximized
   state; rename the toggle to "Show lyrics" / "Compact" or an expand icon (M). This also makes lyrics available without
   maximizing.
3. **Optional lyrics drawer in compact view** (a ▾ toggle under Up Next that expands the window height to show lyrics) (
   M–L).
4. **Keyboard & focus** (S–M): make buttons focus-traversable (or install scene-level key handlers) — Space
   play/pause, ←/→ seek ±5 s, ↑/↓ volume, N next, P previous, L toggle lyrics, F11 fullscreen, Esc back. Add `:focused`
   and `:pressed` styles to `.pw-button`.
5. **Tooltips** on all icon buttons, the speaker (click = mute), the session label ("Time listened in this session") (
   S).
6. **Add Previous**, and a shuffle/repeat indicator if/when those states exist (S–M).
7. **Waveform**: hover preview of time + a thin playhead line; during the first listen draw a subtle "builds as you
   listen" skeleton or a short tooltip (M).
8. **Persist** lyrics size and auto-fit; don't force auto-fit back on each open (S).
9. **Window title** = "Title — Artist" while playing (S) — a useful convenience for the taskbar and Alt-Tab.
10. **Fullscreen / ambient mode** (F11): hide window chrome and auto-hide the cursor and controls after a few seconds of
    inactivity (M).
11. Raise `textDim` alpha to ~0.65 for small text, or bump the size of the small caps labels (S).
12. Volume: filled slider track via `.track` styling or a progress-style background, value tooltip, and a mute toggle (
    S–M).
13. Double-click the artwork area to toggle the compact/expanded layout; right-click lyrics → *Copy* (S).

---

## 12. Preloader (`preloader.fxml`, `PreloaderController`)

- The loader `Circle` is a uniformly stroked circle that **rotates** (`RotateTransition`). A symmetric circle looks
  identical at every angle, so the rotation is invisible. Use an `Arc` or a dashed stroke (`strokeDashArray`) so the
  motion reads, or use a `ProgressIndicator` (S).
- The wave `Path`s are drawn to a hard-coded `x = 800` and anchored left/right, which doesn't stretch a path; at larger
  preloader sizes the waves won't span the width. Bind the end X to the pane width (S).
- Add the app logo (`icon.svg`/`app-icon-128.png` already exist) above "cTunes Loading…" (S).
- If startup takes a long time (DB migrations, scanning), consider a status line ("Opening library…") (S).

---

## 13. Accessibility and general convenience

- **Keyboard navigation** is essentially absent across the app: no accelerators in the menu, no shortcut anywhere, and
  lists are built from `VBox` + mouse handlers, so Tab/arrow navigation doesn't work. Moving to `ListView`/`TableView`
  where possible gets most of this for free.
- **Accessible names**: icon-only buttons need `setAccessibleText` / tooltips for screen readers (S).
- **Color is the only state indicator** for selected/playing/played; add icons or weight changes (S).
- **Contrast**: the lavender 1 px borders and 50%-black subtitles are low-contrast; review against WCAG AA (S).
- **Menu bar**: add mnemonics and accelerators (Ctrl+O/R for rescan, Ctrl+Q quit), and fill *Help › About* with the
  version (`VersionService` already exists) (S).
- **Feedback for failures**: currently almost everything goes to the log only (lyrics not found, tag save errors,
  missing audio file, scan errors). Introduce one lightweight notification mechanism (toast/status bar) and use it
  consistently (M).
- **Remember UI state**: last artist, selected tab, scroll positions, album sort, queue (partially there for window
  size/dividers/volume) (M).
- **Search everywhere**: one global search (Ctrl+F) across artists, albums, tracks with results you can play or queue
  directly (L).
- **Window title** could show the current track when the library window is active too (S).

---

## 14. Small bugs found while reviewing that affect UX

These aren't style issues, but users will notice them:

1. `onBackgroundProcessEvent` overwrites the process name with `""` immediately (`currentTrackName.setText(...)` called
   twice) and uses the player widgets for scan progress.
2. `RescanLibraryController.onChoosePath` throws when the folder picker is cancelled.
3. `LibraryLyricsTabController`: Save is initially enabled and NPEs with no track; the *Enable Edit* toggle never turns
   editing off.
4. `LibraryController.onProgressClicked` throws if nothing is playing (unguarded `Optional.get()`).
5. `LibraryTagEditTabController`: the *Update DB from File* button only logs "Not implemented".
6. Album loading race: rapid artist switching can interleave albums from two artists.
7. `TimeUtils.secondsToHumanTime(null)` returns the string `"-1"`, which would be displayed to the user.
8. Track number displays `null` when the tag is missing.
9. The queue's three context-menu items do nothing.
10. `LAUNCHING` branch shows unformatted time labels (raw numbers) for a moment.

---

## 15. Suggested order of work

1. **Day 1 — quick wins:** §1 table items 1–10, plus fixes from §14. Biggest perceived polish for the least effort.
2. **Next — theme foundation:** tokens in `global.css`, one font scale, icon set (§2). Everything after this is easier
   and more consistent.
3. **Then — interaction:** queue improvements (§6), track list/context menu (§5), status bar and toasts (§3.6, §13).
4. **Then — navigation:** artist search, A–Z, remembered state (§4, §13).
5. **Later — larger redesigns:** transport bar (§3.1), virtualized lists (§4.5), album grid (§5.1), dark theme (§2.4),
   lyrics in compact Now Playing (§11.3), fullscreen mode (§11.10).

---

## 16. Remaining work — steps

**Where things stand.** Done and delivered as patches: quick wins 1–6 and 8–10; the medium items (status bar and
notifications, artist search, queue header/scroll/drag/clear-confirmation, track multi-select + context menu +
now-playing marker, album grid/list, colour tokens); the large items (virtualized artist list with A–Z strip, bottom
transport bar, lyrics drawer + fullscreen in Now Playing). **Skipped on purpose:** dark theme (light scheme only) and
quick win 7 (History time format).

**Important:** none of the changes have been compiled or run (no JavaFX/Maven in the review environment). Step 0 below
comes first for that reason.

Effort tags are as in section 1 (S under an hour, M half a day, L a day or more). Section references point to the detail
above.

### Step 0 — Build and smoke-test what is already delivered (S–M)

Do this before anything else; later steps build on it.

- [ ] Build the project; fix any compile errors (most likely spots: `LibraryController`, `PlaybackWindow`,
  `LibraryArtistAlbumTrackController`, `TrackSelectionModel` users).
- [ ] Start the app and click through:
    - [ ] Artist list: scroll, search, Ctrl+F, Esc, A–Z strip, select an artist, rescan and check the same artist is
      re-selected.
    - [ ] Albums: list/grid toggle, cover-size slider, click a grid card (opens in list), double-click (plays), switch
      artists quickly (no mixed albums).
    - [ ] Tracks: click, Ctrl-click, Shift-click, right-click menu (Play now / Play next / Add to queue / Edit tags),
      double-click, Enter, now-playing marker.
    - [ ] Right-click ▸ Edit tags still jumps to the Edit Tags tab (a listener was accidentally removed and restored
      during the artist-list change).
    - [ ] Queue: header, "↓ Now playing", Delete key, context-menu Remove, Clear confirmation, drag above/below a row,
      drop at the end, edge auto-scroll.
    - [ ] Transport bar: play/pause, next, seek click, mute icon, volume tooltip, click cover/title opens Now Playing, "
      Nothing playing" state.
    - [ ] Status bar: scan progress, Cancel, completion message; "Tags saved", "Lyrics saved", "No lyrics found"
      messages.
    - [ ] Rescan dialog: remembered folder, Enter = Scan, Esc/Cancel, cancelled folder picker, invalid path disables
      Scan.
    - [ ] Now Playing: Space, Esc, L (lyrics drawer), window grows/shrinks with the drawer, F11 fullscreen, controls and
      cursor fade after 3 s, Esc leaves fullscreen first.
- [ ] Note anything that looks wrong; those fixes go ahead of the steps below.

### Step 1 — Finish items that are only partly done (S each)

| #   | Item                                                                                                               | Where                                                  |
|-----|--------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|
| 1.1 | Undo for "Clear queue" (today: confirmation only) — needs a way to restore the queue without disturbing the player | `LibraryPlaylistController`, `PlaylistModel`           |
| 1.2 | Save volume after keyboard changes and after mute/unmute (today: only on mouse release)                            | `LibraryController`, `PlaybackWindow`                  |
| 1.3 | Now Playing: make the speaker icon a mute button and show the volume value in a tooltip                            | `PlaybackWindow`                                       |
| 1.4 | Remove the padded text `" ▶ "` on the play button                                                                  | `library.fxml`, status listener in `LibraryController` |
| 1.5 | Scan result message "Added N tracks, M updated" instead of "finished (N files)" — needs counts from the scanner    | `FilesScanningService`, `LibraryController`            |
| 1.6 | Empty library: add an "Add music folder…" button that opens the Rescan dialog (today: text hint only)              | `LibraryController` (list placeholder)                 |
| 1.7 | Replace the red ⚠ "no lyrics" mark with a dim hint (§5.6)                                                          | `library-album-track.fxml`, `global.css`               |
| 1.8 | Fix the "Disk Number" spelling to "Disc Number" in the tag editor (§5.11)                                          | `library-tab-edit-tags.fxml`                           |
| 1.9 | Replace the `"  ---  "` separator in queue rows with ` · ` (§6.9)                                                  | `PlaylistItemController`                               |

> Exclude 1.1

**Status: DONE (step 1).** 1.1 skipped as requested. Notes: the volume is saved 0.5 s after the last change from any
source (library slider, keyboard, mute, Now Playing slider); the scanner now reports "N added, M updated, K unchanged";
the dead `ArtistSelectionModel` and `ArtistItemBundle` files were deleted.

### Step 2 — Styling foundation (M–L)

Do this before the screen-by-screen steps so new work uses it. See §2.

- [x] 2.1 **Icons instead of emoji** (M): choose an icon source (Ikonli or SVG shapes — *decision needed*, see below),
  replace emoji in buttons, and colour destructive actions through CSS.
- [x] 2.2 **Apply the type scale** (S–M): replace `<Font size=…>` nodes and the remaining `Consolas Bold` (artist row)
  with `.text-small / .text-body / .text-title`.
- [x] 2.3 **Load `global.css` on every Scene** (S): Rescan dialog, About dialog, confirmation dialogs; use
  `getResource(...).toExternalForm()` instead of the relative path in `StageManager`.
- [x] 2.4 **Minimum size for the library window** (S): set a sensible minimum so the three panes cannot collapse (§2.7).
- [x] 2.5 **Seek bar colours from tokens/palette** (S): `ProgressCanvasController` still uses hard-coded orange/grey (
  §3.10).
- [x] 2.6 **Contrast review** (S): lavender borders, dim text on selected/playing rows, `textDim` in Now Playing (
  §11.11, §13).

**Status: DONE (step 2).** Notes:

- Icons: Ikonli 12.4.0 (`ikonli-javafx` + `ikonli-materialdesign2-pack`, added to `build.gradle`). All literals were
  checked
  against the pack's source. `UiIcons` holds the constants/helpers; icons follow the text colour through CSS
  (`.ikonli-font-icon`), and the Now Playing window uses its palette colour (`-pw-text`).
- Type scale: `.text-small/-body/-large/-section/-heading/-title/-strong`; no `<Font>` nodes left in the FXML.
- `UiStyles.apply(...)` puts `global.css` on every Scene and Dialog (Rescan dialog, About, queue confirmation, Now
  Playing).
- Library window minimum size: 900 × 560.
- Seek bar: colours are the `-ct-seek-*` tokens, read back from the stylesheet through invisible probe nodes.
- Contrast: accent `#2563eb` (white text on it ≈ 5:1), inactive selection `#64748b`, danger `#c62f2f`, dim text 62 %
  (Now Playing: 68 %). The lavender row borders are decorative separators and were left as they are.

### Step 3 — Queue leftovers (M)

See §6.

- [x] 3.1 Style for missing-file and skipped tracks (strikethrough/warning icon + tooltip) and a ▶ icon on the playing
  row (§6.5).
- [x] 3.2 Scrollbar `AS_NEEDED` and optional hidden cover in the narrow pane (§6.8).
- [x] 3.3 Keyboard: Alt+↑/↓ move, Ctrl+A select all (§6.10).
- [x] 3.4 Drag several selected rows at once with a count badge (§6.6).
- [x] 3.5 Update the queue incrementally instead of rebuilding (keeps scroll position and selection) (§6.4).
- [x] 3.6 Context menu: *Play next*, *Move to end*, *Show in library*. **Blocked by** the player's preloaded next
  track (see Step 10).

**Status: DONE (step 3).** Notes:

- 3.6 needed the player change from Step 10: the model now publishes a `PlaylistChangedEvent` after every change of
  the queue, and `AudioPlayer` re-preloads the next track when it is no longer the right one
  (reordering, removing, "Play next", "Move to end", shuffle, adding after the current track). This also fixes
  drag-and-drop and Remove, which could leave a stale preloaded track.
- Queue rows are kept when the queue changes (selection, style and scroll position stay); only new tracks get new rows.
  Remove (button, Delete key) now removes all selected rows, not just one.
- Missing-file and skipped tracks have styles (and a tooltip with the path), but nothing sets `FILE_NOT_FOUND` yet —
  that comes with step 9.4.
- Keys in the queue: Alt+↑/↓ move the selected rows, Ctrl+A selects all.
- Context menu: Play next, Move to end, Show in library, Remove from queue.

### Step 4 — Library lists: artists, albums, tracks (M–L)

See §4 and §5.

- [x] 4.1 Artist rows: show the four action buttons only on hover/selection (§4.1).
- [x] 4.2 Generated avatars (initials on a colour from the name) instead of the same placeholder (§4.2).
- [ ] 4.3 Artist sort toggle: A–Z / recently played / most played (history table has the data) (§4.4).
- [ ] 4.4 Remember last artist, selected tab and list scroll across restarts (§4.6, §13).
- [ ] 4.5 Double-click an artist to play it (§4.8).
- [x] 4.6 Albums: always-visible *Play / Add to queue / Shuffle* buttons; stop the previous fade animation before
  starting a new one (§5.2).
- [x] 4.7 Richer album subtitle: year · tracks · duration · genre (§5.3).
- [x] 4.8 Album sort control (year ↑/↓, name), remembered (§5.5).
- [ ] 4.9 Smaller default cover in list view (~200 px) — *decision needed* (today 350 px).
- [x] 4.10 Track rows: ▶ on hover in place of the number, right-aligned fixed-width duration (§5.6).
- [x] 4.11 Track context menu: *Find lyrics*, *Show file in folder*, *Copy title/artist* (§5.7).
- [x] 4.12 Keyboard in the track list: ↑/↓ to move, Shift+Enter to add to queue (§5.9).
- [x] 4.13 Drag tracks/albums from the library into the queue (§5.10).

> Exclude 4.3, 4.4, 4.5, 4.9

**Status: DONE (step 4)**; 4.3, 4.4, 4.5 and 4.9 were excluded as requested. Notes:

- 4.1/4.2: the four artist buttons appear when the pointer is over the row or the row is selected; the avatar is the
  artist's initials on a colour taken from the name (`Avatar`, unit-tested).
- 4.6/4.7: every album has *Play / Add to queue / Shuffle* buttons and a subtitle such as
  `1999 · 12 tracks · 48:12 · Rock`. The hover play overlay no longer stacks animations.
- 4.8: sort *Year (oldest/newest first)* or *Name*, remembered.
- 4.10: the track number turns into a play icon while the pointer is over the row (click it to play); durations are
  right-aligned in a fixed column.
- 4.11: the track menu also has *Find lyrics* (opens the track in the tag editor and searches; you review before
  saving),
  *Show in file manager* and *Copy title and artist*.
- 4.12: Up/Down move the track selection (the list scrolls to it), Shift+Enter adds the selected tracks to the queue.
  Clicking in the album area takes the keyboard from the artist list so that the arrows move between tracks.
- 4.13: tracks (the whole selection) and albums (list cover, or grid card) can be dragged into the queue; they land
  above
  or below the row under the pointer, or at the end.

### Step 5 — Lyrics tab and Edit Tags tab (M)

See §7 and §8.

Lyrics tab:

- [ ] 5.1 Reading mode (clean, borderless, scrolling) vs. edit mode (§7.2).
- [ ] 5.2 "● Unsaved" label and a Revert button (§7.3).
- [ ] 5.3 Spinner / disabled button while searching; show which source answered (§7.4).
- [ ] 5.4 Empty states: "Nothing is playing", "No lyrics — Find lyrics" (§7.5).
- [ ] 5.5 A−/A+ font control, saved on release; share the size and auto-fit settings with Now Playing (§7.6, §7.7).

Edit Tags tab:

- [ ] 5.6 Header with cover, title, artist/album and file path; empty state "Select a track…" (§8.1, §8.2).
- [ ] 5.7 Changed-field tracking: highlight, Save enabled only when changed, Reset/Revert all, Ctrl+S (§8.3).
- [ ] 5.8 ↺ restore button on overridden fields plus a legend explaining the orange border (§8.5).
- [ ] 5.9 Wrap the form in a `ScrollPane`; consistent label widths (§8.6).
- [ ] 5.10 Validation for numeric fields and a date hint (§8.7).
- [ ] 5.11 Change cover (choose/drop/paste) and a placeholder when there is none (§8.9) — M–L.
- [ ] 5.12 Batch edit for several tracks (§8.10) — L, optional.

### Step 6 — History tab (M–L)

The whole section (§9) is untouched. Quick win 7 (time format) was excluded; if it stays excluded, skip 6.2 or keep the
current columns.

- [ ] 6.1 Load off the UI thread with a spinner, limit the rows (e.g. latest 200 + "Load more"), and fetch
  albums/artists in one query instead of one per row (§9.5) — do this first.
- [ ] 6.2 Single "Played at" column (`Today 14:03`, `Yesterday 21:15`, date), full time in a tooltip (§9.2).
- [ ] 6.3 Column widths, sorting, table fills the pane (§9.3).
- [ ] 6.4 Stat cards: time listened, songs, albums, artists (§9.1).
- [ ] 6.5 Row actions: double-click to play, context menu (§9.4).
- [ ] 6.6 Empty state and date filters (§9.6).

> Exclude 6.5

### Step 7 — Now Playing window leftovers (M)

See §11.

- [ ] 7.1 Visible "◀ Library" button with tooltip (§11.1).
- [ ] 7.2 Choose compact/wide layout by window size instead of maximized state (§11.2) — *decision needed*, since the
  lyrics drawer and fullscreen already cover most of the need.
- [ ] 7.3 More keys: ←/→ seek, ↑/↓ volume, N next (P previous after Step 10) (§11.4).
- [ ] 7.4 Waveform: hover time preview, playhead line, hint that it fills in while listening (§11.7). Same for the
  library seek bar (§3.9).
- [ ] 7.5 Window title "Title — Artist" while playing; also for the library window (§11.9, §13).
- [ ] 7.6 Filled volume track; double-click artwork toggles layout; right-click lyrics ▸ Copy (§11.12, §11.13).

### Step 8 — Rescan dialog, Settings, Preloader (S–L)

- [ ] 8.1 Rescan dialog on a proper layout instead of absolute positions; recent-folders list (§10.1, §10.2).
- [ ] 8.2 Settings dialog: library folders, default shuffle behaviour, lyrics font (§10.6) — L, optional.
- [ ] 8.3 Preloader: make the rotation visible (arc or dashed stroke), stretch the waves with the window, add the logo
  and a status line (§12) — S each.

### Step 9 — Accessibility and navigation (M–L)

See §13.

- [ ] 9.1 Menu accelerators and mnemonics (Ctrl+R rescan, Ctrl+Q quit) (S).
- [ ] 9.2 Accessible names for all icon-only buttons (S).
- [ ] 9.3 Do not rely on colour alone for selected/playing/played states (S).
- [ ] 9.4 Notify the user when an audio file is missing (extend the status-bar messages) (S).
- [ ] 9.5 Global search across artists, albums and tracks with play/queue actions (L).
- [ ] 9.6 Remember more UI state: tab, scroll, album sort (overlaps 4.4, 4.8) (M).

### Step 10 — Needs backend/player work first

These cannot be done in the UI layer alone.

| Item                                                                                               | What is needed                                                                                                                                                                                                            |
|----------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Previous track** (§3.4, §11.6)                                                                   | The player only supports "skip next". Needs a notion of "go back" in the queue (or "restart the track").                                                                                                                  |
| **Queue reordering that affects the next track** *(DONE in step 3)* (§6.3: Play next, Move to end) | The player preloads the next track; reordering or removing it leaves the preloaded one stale. Needs the player to re-evaluate the next track when the queue changes. This already affects drag-and-drop and Remove today. |
| **Save the queue across restarts** (§6.11)                                                         | No table exists for it in the migrations (`V001`–`V005`). Needs a schema addition and load/save logic. Confirm first that it really is not stored.                                                                        |
| **Scan result counts** (1.5)                                                                       | The scanner reports progress but not added/updated counts.                                                                                                                                                                |

### Decisions needed from you

1. **Icons (2.1):** add a library (Ikonli) as a dependency, or use SVG shapes in the project? Either way the emoji go
   away.

> Ikonli

2. **Default list-view cover size (4.9):** keep 350 px or reduce to ~200 px?

> Keep 350px

3. **Layout by size in Now Playing (7.2):** still wanted now that the lyrics drawer and fullscreen exist?

> yes

4. **Queue persistence (Step 10):** wanted? It is the only item that adds a database table.

> no

5. **History tab (Step 6):** which parts do you want — all of it, or only the performance fix (6.1) and row actions (
   6.5)?

> see comment in section

6. **Settings dialog (8.2):** worth building now, or later?

> build now

### Suggested order

Step 0 → Step 1 → Step 2 → then by value: Step 3 (queue), Step 4 (library lists), Step 5 (lyrics and tags), Step 6 (
history), Step 7 (Now Playing), Steps 8–9. Step 10 depends on your decisions above and can run in parallel with the UI
steps because it touches different code.