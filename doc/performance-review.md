# Performance review: what to improve, why and how

A pass over the code for slow or wasteful work, memory leaks and a few correctness bugs found along the way,
plus an investigation of how `.m4a` audio is served to the player.

Each item says **where** (file and method), **why** it costs something, and **how** to fix it. Items are numbered so
they can be referred to in commits. Nothing here has been measured on a real library yet: the "impact" column is an
estimate from reading the code. Measure before and after the bigger changes (a simple `System.nanoTime()` log
around artist switching, queue loading and rescans is enough).

## Summary

| #   | Area         | Problem                                                                               | Impact                        | Effort  | Priority |
|-----|--------------|---------------------------------------------------------------------------------------|-------------------------------|---------|----------|
| 1.1 | m4a playback | Every `.m4a` is assumed to be ALAC; an AAC `.m4a` would be sent to the ALAC decoder   | None today (library is ALAC)  | Small   | Low      |
| 1.2 | m4a playback | Decoded audio is streamed without length or byte ranges: seeking and duration suffer  | Seeking, CPU — every `.m4a`   | Medium  | High     |
| 1.3 | m4a playback | The endpoint reads any path, on all network interfaces                                | Security                      | Small   | High     |
| 1.4 | m4a playback | `@Cacheable` on the endpoint caches nothing useful                                    | Misleading                    | Trivial | Low      |
| 2.1 | Album art    | Covers decoded at full size, on the JavaFX thread, once per row/card, never cached    | Memory, UI stalls             | Medium  | High     |
| 2.2 | Artist view  | One query per album when an artist is selected (N+1)                                  | Artist switch time            | Small   | Medium   |
| 2.3 | Artist view  | A new thread per artist click, earlier ones not cancelled                             | Wasted work on fast clicking  | Small   | Medium   |
| 2.4 | Artist view  | One FXML load (plus a Spring bean) per album, disc and **track row**                  | Artist switch time            | Medium  | Medium   |
| 2.5 | Artist view  | List view and grid view both built for every artist, though one is shown              | Artist switch time, memory    | Small   | Medium   |
| 3.1 | Queue        | Queue is a `VBox` of FXML rows: not virtualised                                       | Large queues slow, memory     | Large   | Medium   |
| 3.2 | Queue        | Every row listens to the shared selection list and is never removed: **memory leak**  | Memory grows all session      | Small   | High     |
| 4.1 | Waveform bar | Waveform images rebuilt pixel by pixel, one `Color` object per pixel                  | CPU on the JavaFX thread      | Small   | High     |
| 4.2 | Waveform bar | Its animation timer redraws 60 times a second forever, even paused or minimised       | Constant CPU/GPU use          | Small   | High     |
| 5.1 | Library scan | Every rescan fully re-parses every file, changed or not                               | Rescan time                   | Medium  | High     |
| 5.2 | Library scan | Every statement is its own transaction                                                | Rescan time                   | Small   | Medium   |
| 5.3 | Library scan | A progress event and an INFO log line per file                                        | UI event flood                | Small   | Low      |
| 5.4 | Library scan | The stop flag is not `volatile`: **Stop may be ignored**                              | Correctness                   | Trivial | High     |
| 5.5 | Library scan | One unreadable or non-audio file aborts the whole scan                                | Correctness                   | Small   | High     |
| 5.6 | Library scan | Extension checks are case-sensitive (`.M4A`, `.MP3` rejected)                         | Correctness                   | Trivial | Medium   |
| 6.1 | Caches       | Album cache keyed on the cover bytes, 5 entries, never evicted: slow and can go stale | Scan time, stale data         | Small   | Medium   |
| 6.2 | Caches       | Track-override map shared by scan and UI threads without synchronisation              | Rare corruption               | Small   | Low      |
| 6.3 | Database     | No index on `playback_history.datetime`                                               | History tab on long histories | Trivial | Low      |
| 6.4 | Database     | Album queries always fetch the cover BLOB                                             | Memory, query time            | Small   | Low      |
| 7.1 | Threads      | About ten `new Thread(...)` call sites: unbounded, some non-daemon, errors lost       | Robustness                    | Small   | Medium   |
| 8.1 | Start-up     | Tomcat + Spring MVC + Actuator run only for the audio endpoint                        | Start-up time, memory         | Small   | Medium   |
| 8.2 | Logging      | DEBUG for two packages in the shipped config; INFO per FXML load and per scanned file | Log volume, a little CPU      | Trivial | Low      |

A suggested order is at the end ([Suggested order](#suggested-order)).

---

## 1. Serving `.m4a` audio

### Current design

`MediaProvider.getMedia` turns every file ending in `m4a` into
`http://localhost:<port>/current/track?name=<path>`. `HttpRestController.get` decodes the file with the `alac2wav`
library (`AlacDecoder.decode(WavFormat.RAW_PCM).fromFile(path).toStream(os)`) straight into the HTTP response, and
JavaFX plays the WAV it receives. This exists because JavaFX cannot decode ALAC and its `Media` class accepts only a
URL, not an `InputStream`.

The constraint is real and still current: JavaFX's supported encodings are MP3, AAC and PCM (in WAV/AIFF) — no ALAC,
no FLAC — and the request for playing media from an `InputStream` (JDK-8350282) is still unresolved, with no fix
version. A custom URL protocol handler does not help either: `Media` accepts only `http`, `https`, `file`, `jar`
and `jrt` URLs.

### 1.1 Every `.m4a` is assumed to be ALAC

**Where:** `MediaProvider.getMedia` (`endsWith("m4a")`).

**Status:** not a problem today. The library comes from iTunes and its `.m4a` files are all Apple Lossless (ALAC);
the rest are MP3. So every `.m4a` really does need the decoder, which makes section 1.2 and option A below matter
for every `.m4a` track played.

**Why it is still worth a small check:** `.m4a` is only a container. It can also hold AAC, which is what an iTunes
Store purchase or an AAC import gives you (protected purchases are `.m4p`). JavaFX plays AAC itself, while `alac2wav`
decodes ALAC only, so a single AAC `.m4a` added later would fail to play and the failure would look like a decoder
bug.

**How** (cheap; do it together with the `V006` migration):

1. At scan time read the codec from the sample description box: `moov/trak/mdia/minf/stbl/stsd`, whose first child
   is `alac` or `mp4a`. `MetadataManager.extractM4aMetadata` already walks the box tree with `isoparser`.
2. Store it in a new column, e.g. `TRACK.CODEC` (`mp3`, `aac`, `alac`), via a Flyway migration `V006`.
3. In `MediaProvider`, only `alac` goes through the decoder; `aac` and `mp3` use `Path.toUri()`.

### 1.2 The decoded audio is streamed with no length and no byte ranges

**Where:** `HttpRestController.get` returns a `StreamingResponseBody`.

**Why:**

- JavaFX seeks over HTTP with **byte-range requests**. A streaming body sends one chunked response with no
  `Content-Length` and no `Accept-Ranges`/`206 Partial Content` support. A seek into audio JavaFX has not buffered
  yet then either fails or makes it read again from the start, and the whole track is decoded again.
- With no `Content-Length` the duration may be unknown or wrong until the stream ends. A missing `Content-Length` has
  also crashed JavaFX's GStreamer back-end before (JDK-8094911).
- Replaying a track, or playing it again later, decodes it again from scratch.
- The response is `application/octet-stream`, not `audio/wav`. JavaFX copes by sniffing the bytes, but it's fragile.
- *Check:* `WavFormat.RAW_PCM` keeps the source's bit depth and sample rate. Make sure 24-bit / 96 kHz ALAC files
  play. If they don't, resample to 16-bit / 44.1 kHz or 48 kHz when decoding.

**How:** see [Options](#options-for-playing-alac) below. The recommended option removes the endpoint entirely.

### 1.3 The endpoint serves any path, on every network interface

**Where:** `HttpRestController.get(@RequestParam("name") String name)`; `server.port` with no `server.address`.

**Why:** Spring Boot listens on all interfaces by default, so any device on the same network can call
`/current/track?name=<any path on the disk>`. Only ALAC files decode, which limits the damage, but it is still an open
file-reading endpoint. Actuator adds more endpoints on the same port.

**How:** remove the endpoint (option A below). If HTTP stays: set `server.address: 127.0.0.1`, take a track **id**
instead of a path (look the path up in the database), and remove Actuator (item 8.1).

### 1.4 `@Cacheable` on the endpoint caches nothing useful

**Where:** `@Cacheable(value = "media", key = "#name")` on `HttpRestController.get`.

**Why:** the cached value is the `StreamingResponseBody` lambda, not audio. Every request still decodes the whole file.
The annotation only suggests a cache that does not exist.

**How:** remove it. Real caching is option A.

### Options for playing ALAC

| Option                                                              | Seeking | First play                  | Removes HTTP | Effort | Notes                                                                                                                                                                                                                                                                                               |
|---------------------------------------------------------------------|---------|-----------------------------|--------------|--------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **A. Decode to a WAV file in a disk cache, play it as `file:`**     | Native  | Wait for decode (≈ 0.2–1 s) | Yes          | Medium | **Recommended.** Pre-decode the next queue item so it usually starts at once.                                                                                                                                                                                                                       |
| B. Keep HTTP, but serve a decoded file with `Range` support         | Native  | Same as A                   | No           | Medium | Same cache as A, served as a Spring `Resource` (Spring handles `Range` and `Content-Length` itself). Only if HTTP is needed for something else.                                                                                                                                                     |
| B+. Decode on the fly from any byte offset (in `alac2wav`)          | Native  | Instant                     | No           | Large  | ALAC's sample tables (`stsz`/`stco`) map sample positions to packets, so a WAV byte range can be decoded from the nearest packet. Elegant, no disk use, but the most work. `alac2wav` is your own library, so it's possible.                                                                        |
| C. Custom URL protocol / `InputStream` for `Media`                  | —       | —                           | —            | —      | Not possible: `Media` only accepts http(s)/file/jar/jrt, and JDK-8350282 is unresolved.                                                                                                                                                                                                             |
| D. Different audio engine (FFmpeg via JavaCV, or vlcj) + Java Sound | Native  | Instant                     | Yes          | Large  | Plays everything (ALAC, FLAC, Opus), allows gapless playback, and Java Sound reports the exact output position, so the visualiser could sync **without calibration**. Loses JavaFX's spectrum: you'd compute your own FFT (e.g. JTransforms). vlcj needs VLC installed. A rewrite of `AudioPlayer`. |
| E. Convert the library's ALAC to AAC                                | Native  | Instant                     | Yes          | Small  | Lossy: not acceptable for a lossless library.                                                                                                                                                                                                                                                       |

#### Recommended: option A, step by step

1. **Codec column** (item 1.1), optional for an all-ALAC library but cheap, so only ALAC is ever decoded.
2. **`PcmCache` component** (domain layer):
    - Directory: `~/ctunes/cache/pcm/`; file name `<trackId>-<audioMd5>.wav`. `audio_md5` already exists, so an edited
      or replaced file gets a new cache entry automatically.
    - `CompletableFuture<Path> get(TrackRecord)`: returns at once if the file exists. Otherwise it decodes on a
      background executor to `<name>.part`, then does an atomic `Files.move` to the final name, so a crash never leaves
      a half-written WAV that looks complete.
    - Size limit, e.g. 2 GB (a user preference), evicting the least recently used files. Touch the file's
      last-modified time on each use.
    - Sizes to plan for: iTunes ALAC rips from CD are 16-bit / 44.1 kHz, which is about 10.6 MB of WAV per minute
      (about 40 MB for a 4-minute track), so 2 GB holds roughly 50 tracks: plenty for the current track, the next few
      in the queue and recent replays. Hi-res ALAC (24-bit / 96 kHz) is about 34.6 MB per minute.
3. **`MediaProvider`** becomes asynchronous for ALAC: `AudioPlayer` asks for the media, shows the existing loading
   animation (`startLoadingAnimation`) while the future completes, then creates `new Media(path.toUri().toString())`
   on the JavaFX thread.
4. **Pre-decode the next track:** when a track starts, call `pcmCache.get(next)` for
   `AudioPlayer.getNextPlaylistItemProperty()`, so the next ALAC track is usually ready before it's needed.
5. **Remove the web layer:** delete `HttpRestController` and the HTTP branch of `MediaProvider`; remove
   `spring-boot-starter-web` and `spring-boot-starter-actuator`, `server.port` and the `SERVER_PORT` variable; set
   `spring.main.web-application-type: none`. The H2 console needs a servlet container, so keep it (and the web
   starter) in a `dev` profile only, or use the H2 shell or an IDE database view instead.
6. **Bonus — the whole waveform at once:** while decoding, keep the peak of every 1/2000 of the track. The waveform
   bar can then show the whole track from the first second instead of filling in as it plays (`WaveformModel`
   already has 2000 bars). For MP3/AAC it keeps filling in as today.

---

## 2. Album art and the artist view

### 2.1 Covers decoded at full size, on the JavaFX thread, again and again

**Where:** `ImageUtils.setImageIfPresent` (`new Image(byteArrayInputStream)`, no size), called from
`LibraryArtistAlbumController.initialize`, `AlbumCard`, `PlaylistItemController.initialize` and the tag editor.
`AlbumCard` also loads `/img/noimage.png` again for every card.

**Why:** an embedded cover is often 1000–3000 px square. Decoded, a 1400 × 1400 cover takes about 7.8 MB of memory
(4 bytes per pixel), even when it is shown at 48 px in a queue row. A queue of 300 tracks from 20 albums decodes
300 covers, not 20. Decoding also happens on the JavaFX thread, which shows up as pauses while scrolling or
switching artists.

**How:**

- One `CoverCache` component: key `(albumId, size)`, value `Image`, held in a Caffeine cache with a weight limit
  (e.g. 100 MB, weighed by width × height × 4) or with soft values.
- Decode at the display size: `new Image(in, size, size, true, true)` (preserve ratio, smooth), as
  `PlaybackWindow.toImage`
  already does. Round sizes to a few steps (64, 128, 256, 512) so slider changes reuse cached images.
- Decode off the JavaFX thread: show the placeholder first, then set the image when the background decode finishes.
  `new Image(...)` is safe to create on a background thread.
- One shared placeholder `Image` for the whole app.

### 2.2 One query per album when an artist is selected

**Where:** `LibraryController` artist selection (around line 385): `getArtistAlbums`, then `getAlbumTracks` inside the
loop.

**Why:** 1 + N round trips. Each is cheap with H2 embedded, but they add up for artists with many albums, and each
returns full track rows.

**How:** one query for all of the artist's tracks — `DataAccess.getAlbumsTracks(albums)` already exists — then group by
`FK_ALBUM` in memory.

### 2.3 A new thread per artist click

**Where:** same method: `new Thread(() -> …).start()`; only the final `showAlbums` checks `requestId`.

**Why:** clicking down the artist list fires a thread per artist. Each loads every album, disc and track FXML in full,
only for all but the last result to be thrown away.

**How:** run on a shared executor (item 7.1); keep the `Future` of the current load and `cancel(true)` it when another
artist is chosen. Inside the loop, `if (requestId != albumsRequestId.get()) return;` before each album.

### 2.4 One FXML load per album, disc and track row

**Where:** `LibraryController` → `LibraryArtistAlbumController` → `LibraryArtistAlbumDiscController` → one
`LIBRARY_ARTIST_ALBUM_TRACK` FXML per track; `FxmlLoader.load` resolves every controller with `context::getBean` and
logs at INFO each time.

**Why:** an FXML load (parse XML, reflection, CSS) plus a Spring prototype bean costs roughly 1–5 ms. An artist with
20 albums of 12 tracks is about 260 loads, so 0.3–1.3 s before the albums appear. `FxmlLoader` also never closes the
`InputStream` it opens.

**How:**

- Build the track row in plain Java (an `HBox` with labels), or show each disc's tracks in a `TableView`/`ListView`
  with a cell factory, which only creates cells for visible rows and reuses them while scrolling.
- Keep FXML for the album header, where it's one per album.
- In `FxmlLoader`: log at DEBUG; use try-with-resources for the stream.

### 2.5 Both album views built for every artist

**Where:** `LibraryController.showAlbums` builds both the list nodes and an `AlbumCard` per album, whichever view is
shown.

**Why:** double the nodes and (until 2.1) double the cover decodes.

**How:** build the visible view only; build the other the first time the user switches to it, and keep it for that
artist.

---

## 3. The queue (playlist)

### 3.1 The queue is not virtualised

**Where:** `LibraryPlaylistController`: a `VBox` (`playlistVbox`) with one `PLAYLIST_ITEM` FXML per track.

**Why:** every track in the queue has its own FXML load, node tree and cover, whether visible or not. Adding a big
artist or "play all" means thousands of loads and nodes, and layout slows as the list grows.

**How:** replace the `VBox` with a `ListView<PlaylistItem>` and a custom `ListCell`. Only the visible rows exist; cells
are reused as you scroll. Drag-and-drop reordering, multi-selection and styling move into the cell (`ListView`
supports all of them). This is the biggest change in this document, so do it after 3.2 and 2.1, which remove most of
the memory cost on their own.

### 3.2 Removed queue rows are never released (memory leak)

**Where:** `PlaylistItemController.initialize`:
`playlistSelectionModel.getSelectionModel().getSelectedItems().addListener(this::onPlaylistSelectionChanged)`.
There is no `removeListener` anywhere in the code.

**Why:** the shared selection list holds every row's listener, so it holds every row controller, its node tree and its
decoded cover, for the whole session. That includes rows long since removed from the queue. Each selection change
also notifies every row ever created. Each runs `change.getRemoved().contains(...)` over the changed items, so "select
all" on a big queue is quadratic.

**How:** give `PlaylistItemController` a `dispose()` that removes the listener, and call it when
`LibraryPlaylistController` drops a row (in the sync method that rebuilds `rows`). Alternatively, wrap the listener
in a `WeakListChangeListener` held in a field. Better still: keep one listener in `LibraryPlaylistController` that
updates only the rows that changed (or let the `ListView` from 3.1 handle selection).

---

## 4. The waveform bar in the library window

### 4.1 Waveform images built pixel by pixel

**Where:** `ProgressCanvasController.renderImage`, called twice per rebuild (played and unplayed images).

**Why:** it fills the whole background with `setArgb` one pixel at a time. For each bar pixel it then calls
`Color.interpolate`, which **allocates a new `Color` object per pixel**, and converts it back to ARGB. For a
1200 × 60 canvas that is about 144 000 pixel writes and up to 144 000 allocations per image, two images per rebuild,
up to 10 rebuilds a second, all on the JavaFX thread. Meanwhile only **one bar** changes between two spectrum updates.

**How** (any one of these helps; the first is the quickest):

- Precompute the gradient once per height: `int[] rowColor = new int[height]` (top → bottom). Build one `int[]` for
  the whole image and write it with a single
  `PixelWriter.setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), pixels, 0, w)`.
  No per-pixel object, no per-pixel call.
- Update incrementally: on a spectrum update, redraw only the pixel column(s) of the bar that changed.
- Or drop the images: draw the bars with `GraphicsContext.fillRect` and a `LinearGradient` paint. There are 2000 bars
  but only ~1200 pixel columns, so aggregate bars to columns (max per column) first.

### 4.2 The waveform bar redraws 60 times a second, forever

**Where:** `ProgressCanvasController.startProgress`: the `AnimationTimer` is started once and never stopped; each frame
calls `render()`, which clears the canvas and draws both images and the playhead.

**Why:** the canvas is redrawn about 60 times a second even when playback is paused, nothing has changed, or the
window is minimised or hidden behind the playback window. That's a steady background CPU/GPU cost, which matters on
laptops.

**How:** redraw only when something changed. Keep the last drawn playhead x, hover x and images version; in `handle`,
return early when all are unchanged. Stop the timer when playback is paused or stopped (and the loading animation is
not running), and start it again on play.

---

## 5. Library scan (`FilesScanningService`)

### 5.1 Every rescan re-parses every file

**Where:** `extractMetadataAndUpdateDatabase` runs `metadataManager.getMetadata(file)` for every file, then compares
with the database.

**Why:** parsing is the expensive part. `mp3agic`'s `Mp3File` reads the **whole file** to count frames and compute
the duration. The m4a path walks the full box tree, cover included. For an unchanged library of a few thousand files,
almost all of that work is thrown away.

**How:** store each file's size and last-modified time in `TRACK` (two columns, migration `V006`). In the scan, read
them with `Files.readAttributes(file, BasicFileAttributes.class)` — the file visitor already receives them — and skip
files whose size and time match. Let the user force a full rescan from the rescan dialog, for when a parser or
lyrics-cleaning rule changes.

### 5.2 Every statement is its own transaction

**Where:** the scan calls `getOrInsertArtist`, `getOrInsertAlbum`, `getTrack`, `insertTrack` / `update()` and possibly
`storeTrackOverride` with jOOQ's default auto-commit.

**Why:** 4–6 commits per file. Each commit on a file database is a write to disk. Batching is usually several times
faster.

**How:** process files in batches of about 200 inside `dsl.transaction(cfg -> …)` (using `DSL.using(cfg)` inside).

### 5.3 A progress event and a log line per file

**Where:** `publishInProgressStatusEvent` and `log.info("\t{}", file)` for every file.

**Why:** thousands of UI updates (each a hop to the JavaFX thread) and log lines per scan.

**How:** publish progress at most every 100 ms or every 1 %, and always at the end. Log each file at DEBUG.

### 5.4 The stop flag is not `volatile`

**Where:** `private boolean shouldInterrupt;`, written by the event listener thread and read by the `rescan` thread.

**Why:** without `volatile` (or another happens-before edge), the Java memory model allows the scan thread to never see
the change. The JIT may even lift the read out of the loop, so **Stop can be ignored**.

**How:** `private volatile boolean shouldInterrupt;` (or an `AtomicBoolean`).

### 5.5 One bad file aborts the whole scan

**Where:** `doScan` collects **every** regular file; `MetadataManager.getMetadata` throws
`IllegalArgumentException("Unsupported file format")` for anything else and rethrows parse errors; the loop has no
per-file `try`, so the outer `catch` ends the scan.

**Why:** a `cover.jpg`, `folder.ini`, `.cue` or one damaged MP3 anywhere in the library stops the scan at that point,
and everything after it is never scanned.

**How:** in `doScan`, keep only supported extensions. In the loop, catch exceptions per file, log them, count them as
"failed", and show the count in the completion message ("… 3 failed — see log").

### 5.6 Extension checks are case-sensitive

**Where:** `MetadataManager.getMetadata`: `pathString.endsWith("m4a")` / `endsWith("mp3")`. `MediaProvider` lowercases,
so the two disagree.

**Why:** `Song.MP3` or `Track.M4A` is rejected at scan time (and, until 5.5 is fixed, aborts the scan).

**How:** one helper, e.g. `AudioFormat.of(Path)`, that lowercases the extension, used by the scanner, `MetadataManager`
and `MediaProvider`.

### 5.7 Optional: parse in parallel

After 5.1, only new or changed files are parsed. If a first scan of a big library is still slow, parse on 2–4
threads (parsing is mostly I/O) and keep database writes on one thread, in batches.

---

## 6. Caches and the database

### 6.1 The album cache is keyed on the cover bytes

**Where:** `DataAccess.getOrInsertAlbum(Integer artistId, String album, byte[] image)` with `@Cacheable(Caches.ALBUMS)`;
`getOrInsertArtist` with `@Cacheable(Caches.ARTISTS)`; all caches share `maximumSize=5,expireAfterAccess=600s`.

**Why:**

- The cache key includes the `byte[]` cover, so every call computes a deep hash of the whole image (often hundreds of
  KB), and tracks of one album with slightly different embedded art never hit.
- With 5 entries the cache rarely helps anyway.
- Nothing evicts it. After the tag editor renames or merges an artist or album (it also calls these methods), a later
  call within 10 minutes can return the **old record**, possibly for a row that no longer exists.

**How:** remove both `@Cacheable`s. During a scan, use a plain `HashMap<(artistId, name), AlbumRecord>` local to that
scan, which is fast and cannot go stale across edits. If a cache stays, key it on `(artistId, name)` only and
`@CacheEvict(allEntries = true)` after tag edits and scans.

### 6.2 The track-override map is shared by two threads without synchronisation

**Where:** `DataAccess.trackOverrides` (lazy `HashMap`), written by `storeTrackOverride` from the scan thread
(`autoFixLyrics`) and from the JavaFX thread (tag editor, lyrics tab), read by `getTrackOverrides`.

**Why:** concurrent `HashMap` writes can lose entries or, rarely, corrupt the map. The first call also loads the
whole `TRACK_OVERRIDES_HISTORY` table.

**How:** query per track (`WHERE track_id = ?` — the index `idx_overrides_track_id` exists) and drop the map; or make it
a
`ConcurrentHashMap` with `CopyOnWriteArrayList` values.

### 6.3 No index on `playback_history.datetime`

**Where:** `getHistoryStats(since)` and `getHistoryRows(since, …)` filter and order by `PLAYBACK_HISTORY.DATETIME`; the
only index is on `fk_track`.

**Why:** a full table scan on every History tab load. Fine today, slower as the history grows (it never shrinks).

**How:** migration `V006`: `CREATE INDEX playback_history_datetime_index ON playback_history (datetime);`

### 6.4 Album queries always fetch the cover BLOB

**Where:** every `selectFrom(ALBUM)` / `fetch(ALBUM, …)` (artist view, search, queue building).

**Why:** the cover comes along even when only the name or id is needed. Fine for now, since one album record is
shared by its tracks; it grows with the library.

**How:** select the columns you need where the cover isn't shown, and load covers through the `CoverCache` from 2.1
by album id. Longer term, move covers to their own table.

---

## 7. Threads

### 7.1 About ten raw `new Thread(...)` call sites

**Where:** `LibraryController` (2), `SearchController`, `LibraryLyricsTabController`, `LibraryHistoryTabController`,
`LyricsService` (2), `FilesScanningService`, `PlaybackWindow` — only some call `setDaemon(true)`.

**Why:** no limit (a burst of clicks gives a burst of threads), no cancellation, exceptions go to the default handler
and are easy to miss, and a non-daemon thread still running keeps the JVM alive after the window closes.

**How:** one Spring bean, e.g. `BackgroundTasks`, wrapping `Executors.newVirtualThreadPerTaskExecutor()` (Java 21+) and
shut down on context close. Give it a helper `submit(Supplier<T> work, Consumer<T> onFxThread)` that logs failures. It
returns a `Future`, so callers like the artist view (2.3) can cancel the previous task.

---

## 8. Start-up and logging

### 8.1 Web server and Actuator only for the audio endpoint

**Where:** `build.gradle`: `spring-boot-starter-web`, `spring-boot-starter-actuator`; `application.yaml`: `server.port`,
`web-application-type: servlet`.

**Why:** embedded Tomcat, Spring MVC and Actuator add start-up time (typically a second or more) and tens of MB of
memory, and open a network port (see 1.3), all for one internal endpoint.

**How:** after option A (section 1), remove both starters and set `spring.main.web-application-type: none`. Keep the H2
console in a `dev` profile only, if you still want it.

### 8.2 Verbose logging in the shipped config

**Where:** `application.yaml` sets `ui.controller.element` and `model.playlist` to DEBUG; `FxmlLoader` logs INFO per
load; the scanner logs INFO per file.

**Why:** a lot of log output in normal use. Each line also costs formatting and I/O.

**How:** default to INFO in `application.yaml`; move the DEBUG levels to a `dev` profile (`application-dev.yaml`); make
the
per-load and per-file lines DEBUG.

---

## Suggested order

1. **Quick correctness fixes** (an hour): 5.4 `volatile`, 5.5 per-file errors and extension filter, 5.6 case, 1.4 and
   6.1 caches, 1.3 `server.address: 127.0.0.1` as a stop-gap.
2. **Steady CPU and memory** (a day): 4.2 redraw only on change, 4.1 waveform image building, 3.2 queue listener
   leak, 2.1 cover cache at display size.
3. **m4a** (a few days): option A (cache, pre-decoding, removing the web layer, 8.1), with the full-waveform bonus.
   Every `.m4a` in the library is ALAC, so this improves seeking and start-up for most of what you play. Add the 1.1
   codec column in the same migration as a safety net.
4. **Scan speed**: 5.1 skip unchanged files, 5.2 batched transactions, 5.3 throttled progress, 6.3 index (one `V006`
   migration can carry the columns for 1.1 and 5.1 and the index).
5. **Larger UI work**: 2.2–2.5 artist view, 7.1 shared executor, then 3.1 virtualised queue.
