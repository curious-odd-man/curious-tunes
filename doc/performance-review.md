# Performance review: what to improve, why and how

A pass over the code for slow or wasteful work, memory leaks and a few correctness bugs found along the way,
plus an investigation of how `.m4a` audio is served to the player.

Each item says **where** (file and method), **why** it costs something, and **how** to fix it. Items are numbered so
they can be referred to in commits. Nothing here has been measured on a real library yet: the "impact" column is an
estimate from reading the code. Measure before and after the bigger changes (a simple `System.nanoTime()` log
around artist switching, queue loading and rescans is enough).

## Summary

| #   | Area         | Problem                                                                                             | Impact                        | Effort  | Priority |
|-----|--------------|-----------------------------------------------------------------------------------------------------|-------------------------------|---------|----------|
| 1.1 | m4a playback | Every `.m4a` is assumed to be ALAC; an AAC `.m4a` would be sent to the ALAC decoder                 | None today (library is ALAC)  | Small   | Low      |
| 1.2 | m4a playback | Decoded audio is streamed without length or byte ranges: seeking and duration suffer                | Seeking, CPU — every `.m4a`   | Medium  | High     |
| 1.3 | m4a playback | The endpoint reads any path, on all network interfaces                                              | Security                      | Small   | High     |
| 1.4 | m4a playback | `@Cacheable` on the endpoint caches nothing useful                                                  | Misleading                    | Trivial | Low      |
| 2.1 | Album art    | Covers decoded at full size, on the JavaFX thread, once per row/card, never cached                  | Memory, UI stalls             | Medium  | High     |
| 2.2 | Artist view  | One query per album when an artist is selected (N+1)                                                | Artist switch time            | Small   | Medium   |
| 2.3 | Artist view  | A new thread per artist click, earlier ones not cancelled (→ async framework `Flow`)                | Wasted work on fast clicking  | Small   | Medium   |
| 2.4 | Artist view  | One FXML load (plus a Spring bean) per album, disc and **track row**                                | Artist switch time            | Medium  | Medium   |
| 2.5 | Artist view  | List view and grid view both built for every artist, though one is shown                            | Artist switch time, memory    | Small   | Medium   |
| 3.1 | Queue        | Queue is a `VBox` of FXML rows: not virtualised                                                     | Large queues slow, memory     | Large   | Medium   |
| 3.2 | Queue        | Every row listens to the shared selection list and is never removed: **memory leak**                | Memory grows all session      | Small   | High     |
| 4.1 | Waveform bar | Waveform images rebuilt pixel by pixel, one `Color` object per pixel                                | CPU on the JavaFX thread      | Small   | High     |
| 4.2 | Waveform bar | Its animation timer redraws 60 times a second forever, even paused or minimised                     | Constant CPU/GPU use          | Small   | High     |
| 5.1 | Library scan | Every rescan fully re-parses every file, changed or not                                             | Rescan time                   | Medium  | High     |
| 5.2 | Library scan | Every statement is its own transaction                                                              | Rescan time                   | Small   | Medium   |
| 5.3 | Library scan | A progress event and an INFO log line per file                                                      | UI event flood                | Small   | Low      |
| 5.4 | Library scan | The stop flag is not `volatile`: **Stop may be ignored**                                            | Correctness                   | Trivial | High     |
| 5.5 | Library scan | One unreadable or non-audio file aborts the whole scan                                              | Correctness                   | Small   | High     |
| 5.6 | Library scan | Extension checks are case-sensitive (`.M4A`, `.MP3` rejected)                                       | Correctness                   | Trivial | Medium   |
| 6.1 | Caches       | Album cache keyed on the cover bytes, 5 entries, never evicted: slow and can go stale               | Scan time, stale data         | Small   | Medium   |
| 6.2 | Caches       | Track-override map shared by scan and UI threads without synchronisation                            | Rare corruption               | Small   | Low      |
| 6.3 | Database     | No index on `playback_history.datetime`                                                             | History tab on long histories | Trivial | Low      |
| 6.4 | Database     | Album queries always fetch the cover BLOB                                                           | Memory, query time            | Small   | Low      |
| 6.5 | Library scan | A rescan never updates an existing album's cover                                                    | Correctness (stale covers)    | Small   | Medium   |
| 7.1 | Threads      | About ten `new Thread(...)` call sites: unbounded, some non-daemon, errors lost (→ async framework) | Robustness                    | Medium  | Medium   |
| 7.2 | Threads      | Changes the async framework needs before it moves over                                              | Robustness                    | Small   | Medium   |
| 8.1 | Start-up     | Tomcat + Spring MVC + Actuator run only for the audio endpoint                                      | Start-up time, memory         | Small   | Medium   |
| 8.2 | Logging      | DEBUG for two packages in the shipped config; INFO per FXML load and per scanned file               | Log volume, a little CPU      | Trivial | Low      |

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
the rest are MP3. So every `.m4a` really does need the decoder, which makes section 1.2 and the chosen streaming
design (option G) below matter
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

**How:** solved by the chosen
design ([option G](#decision-stream-alac-straight-into-javafx-through-a-custom-source-option-g)), which removes the
endpoint. This also matches how JavaFX's HTTP source seeks: it reopens the connection with a `Range` header and only
succeeds on `206 Partial Content`; otherwise it falls back to downloading everything.

### 1.3 The endpoint serves any path, on every network interface

**Where:** `HttpRestController.get(@RequestParam("name") String name)`; `server.port` with no `server.address`.

**Why:** Spring Boot listens on all interfaces by default, so any device on the same network can call
`/current/track?name=<any path on the disk>`. Only ALAC files decode, which limits the damage, but it is still an open
file-reading endpoint. Actuator adds more endpoints on the same port.

**How:** remove the endpoint (option G below). Until then, as a stop-gap: set `server.address: 127.0.0.1`, take a track
**id**
instead of a path (look the path up in the database), and remove Actuator (item 8.1).

### 1.4 `@Cacheable` on the endpoint caches nothing useful

**Where:** `@Cacheable(value = "media", key = "#name")` on `HttpRestController.get`.

**Why:** the cached value is the `StreamingResponseBody` lambda, not audio. Every request still decodes the whole file.
The annotation only suggests a cache that does not exist.

**How:** remove it. With option G nothing needs caching.

### Decision: stream ALAC straight into JavaFX through a custom source (option G)

**Chosen:** option G below. A small patch to JavaFX's media module lets the player read a *virtual WAV file*: the
ALAC track presented as uncompressed PCM, decoded on demand, packet by packet, as the player asks for bytes. No HTTP
server, no temporary files, no decoding of the whole track up front.

**Why this one:**

- **Instant playback.** Only the first packets (about 0.1 s of audio each) are decoded before sound starts, whatever
  the track length.
- **Instant, exact seeking.** The virtual WAV has a known length, so JavaFX knows the duration at once and seeks by
  byte offset. A byte offset maps straight to a sample, and ALAC's sample tables map a sample to a packet, so a seek
  decodes one packet, not the track.
- **Nothing written, little memory.** Nothing goes to disk; memory use is a few decoded packets, not a 40 MB track.
- **Removes the web layer.** The HTTP endpoint, Tomcat, Actuator and the open port all go (items 1.2–1.4 and 8.1 are
  solved by this).
- **It's an interesting piece of engineering**, and the decoder is our own library (`alac2wav`), so the seekable
  decoding it needs can be added where it belongs.

**What it costs**, accepted knowingly:

- It depends on **internal JavaFX classes** (`com.sun.media.jfxmedia.locator`). Every JavaFX upgrade needs the patch
  re-checked against the new source. JavaFX is pinned to `25.0.1` in `build.gradle`, which helps.
- The app must always start with the right JVM arguments (or class-path order) so the patch is in place.
- Only tested on Windows, where JavaFX uses its GStreamer back-end. macOS uses a different native path that has had
  bugs with Java-side sources (JDK-8357714, 2025), so other platforms would need their own testing.
- More code to own than a cache. To limit the risk, the app keeps a fallback (see step 6).

### Options considered

| Option                                                                             | Seeking           | First play                                                                | Removes HTTP | Effort    | Why not chosen                                                                                                                                                                                                                                                                                                                                         |
|------------------------------------------------------------------------------------|-------------------|---------------------------------------------------------------------------|--------------|-----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| A. Decode to a WAV file in a disk cache, play it as `file:`                        | Native            | Wait for a full decode (≈ 0.2–1 s); hidden by pre-decoding the next track | Yes          | Medium    | The safest option (public API only), and still the **fallback**. But it decodes whole tracks and writes files, and starts are only instant when the track was pre-decoded. Disk wear was considered and is negligible (a 600 TBW SSD would last centuries at this rate), so it isn't the reason.                                                       |
| B. Keep HTTP, serve a decoded file with `Range` support                            | Native            | Same as A                                                                 | No           | Medium    | Same work as A, plus keeping Tomcat and a local port.                                                                                                                                                                                                                                                                                                  |
| B+. Keep HTTP, decode on the fly from any byte offset                              | Native            | Instant                                                                   | No           | Large     | Needs the same seekable decoder as G, but keeps the whole web layer. G gets the same result without HTTP.                                                                                                                                                                                                                                              |
| C. Custom URL protocol handler for `Media`                                         | —                 | —                                                                         | —            | —         | Not possible: `Media` only accepts http(s)/file/jar/jrt URLs.                                                                                                                                                                                                                                                                                          |
| D. Different audio engine (FFmpeg via JavaCV, or vlcj) + Java Sound                | Native            | Instant                                                                   | Yes          | Large     | A rewrite of `AudioPlayer`, and loses JavaFX's spectrum (we'd need our own FFT). Its one big advantage — exact output position, so the visualiser syncs without calibration — doesn't outweigh that today. Worth revisiting if gapless playback or FLAC is ever wanted.                                                                                |
| E. Convert the library's ALAC to AAC                                               | Native            | Instant                                                                   | Yes          | Small     | Lossy: not acceptable for a lossless library.                                                                                                                                                                                                                                                                                                          |
| F. Register a fully decoded WAV in JavaFX's internal memory cache (`LocatorCache`) | Native            | Wait for a full decode                                                    | Yes          | Small     | No patch needed: `Locator` checks this cache first and plays from memory. But it still decodes the whole track before playing, holds ~40 MB per track, and uses the same internal API as G, so it has G's upgrade risk without G's instant start. A good **first experiment** to prove that a Java-side source works with our JavaFX version (step 0). |
| **G. Custom streaming source (`ConnectionHolder`) via a patched `Locator`**        | **Native, exact** | **Instant**                                                               | **Yes**      | **Large** | **Chosen.**                                                                                                                                                                                                                                                                                                                                            |

### How JavaFX reads media (what the patch hooks into)

`Media` creates an internal `com.sun.media.jfxmedia.locator.Locator` for the URL. `Locator.init()` works out the
content type and length, and `Locator.createConnectionHolder()` returns a `ConnectionHolder`, the object the native
player pulls bytes from. The built-in holders are file, HTTP, HLS and memory. In JavaFX's source (checked in
version 13; re-check against 25.0.1), `ConnectionHolder` has:

- package-private abstract methods `readBlock(long position, int size)`, `needBuffer()`, `isSeekable()`,
  `isRandomAccess()`, plus a public abstract `seek(long position)`;
- `readNextBlock()` (sequential reads through a `ReadableByteChannel channel` into a 4 KB direct `ByteBuffer buffer`),
  `getBuffer()`, `closeConnection()`, `property(int, int)`, `getStreamSize()`.

A holder that says `isRandomAccess() == true` (like the file and memory holders) is read with `readBlock(position,
size)`: it puts the bytes in `buffer` and returns how many it put there. That positional read is exactly what an
on-demand decoder needs.

Because those methods are **package-private**, our holder must live in the same package, inside the `javafx.media`
module. Hence a patch, not just reflection.

### Design

```
 AudioPlayer ──▶ MediaProvider ──▶ new Media("file:///ctunes-stream/<trackId>.wav")
                       │
                       └─ registers AlacWavSource for <trackId> in StreamSources
                                                       ▲
 javafx.media (patched)                                │ looks up by URI
   Locator.init()               ── our URI? ──▶ length + "audio/x-wav" from the source, no file check
   Locator.createConnectionHolder() ── our URI? ──▶ new StreamConnectionHolder(source)
   StreamConnectionHolder.readBlock(pos, size) ──▶ source.read(pos, buffer)
                                                       │
 app: AlacWavSource (virtual WAV = 44-byte header + PCM)
   read(pos): pos ▶ sample ▶ ALAC packet(s) ▶ decode (small LRU of packets) ▶ copy bytes
                                                       │
 alac2wav: SeekableAlacDecoder (packet index from stsz / stco / stsc / stts, magic cookie from stsd)
```

The patch contains only JavaFX-side plumbing and no app logic:

- `RandomAccessSource` — a small **public** interface in the patched package (`long length()`,
  `int read(long position, ByteBuffer target)`, `String contentType()`, `void close()`).
- `StreamSources` — a public static registry: `register(URI, RandomAccessSource)`, `unregister(URI)`, `lookup(URI)`.
- `StreamConnectionHolder extends ConnectionHolder` — delegates to a `RandomAccessSource`; `isRandomAccess()`,
  `isSeekable()` → true; `needBuffer()` → false; `readBlock` fills `buffer`; `seek` checks the bounds;
  `closeConnection` → `source.close()`.
- `Locator` — a copy of JavaFX 25.0.1's `Locator.java` with **two small edits**, both guarded by
  `StreamSources.lookup(uri) != null`: in `init()`, take the length and content type from the source and return
  early, before the file check and file-signature sniffing; in `createConnectionHolder()`, return a
  `StreamConnectionHolder`. Every other URI behaves exactly as before.

The URI uses the `file` scheme with a path that never exists on disk (`file:///ctunes-stream/<trackId>.wav`). That
way it passes `Media`'s protocol check and `.wav` gives the right content type; the patched `init()` handles it
before anything touches the disk.

### Implementation plan

**Status: built** (step 3 of the suggested order), with these differences from the plan below:

- The seekable decoder lives in the app, `domain/audio/alac` (`Mp4AlacTrack`, `AlacPacketDecoder`,
  `AlacVirtualWav`), not in `alac2wav`: that repository wasn't reachable. It has no dependencies, so it can move
  there later. It is tested bit for bit against FFmpeg's decoding of FFmpeg-made files: 16-bit stereo and 24-bit
  mono, compressed and uncompressed packets, silence runs, and random reads. It decodes about 200× faster than
  real time.
- The patch is a `jfxPatch` source set in this project (`src/jfxPatch/java`), with the original JavaFX
  25.0.1 files in `src/jfxPatch/original/` for diffing. It was compile-checked and exercised against JavaFX 25.0.1's
  real `Locator` and `ConnectionHolder` sources (pull and push reads, seek, close, unregistered URIs).
- Step 0 (the spike) was folded into the real thing: `AlacStreaming.isAvailable()` checks at run time that the
  patch is active and otherwise logs a warning and uses the fallback.
- The fallback is the existing HTTP endpoint, so step 7 (removing the web layer) is not done.
- **Still to verify on Windows:** an ALAC track plays, seeks and moves on to the next track; the log says
  `ALAC streaming: JavaFX media patch 25.0.1 active`, in both `gradle run` and the deployed jar.


0. **Spike (half a day): prove a Java-side source plays.** Before writing the decoder, try option F: decode one
   track fully, register it with `LocatorCache.locatorCache().registerURICache(...)` under a fake URI, and play it.
   If it plays and seeks on JavaFX 25.0.1 on Windows, the native side accepts Java-side random-access sources and the
   rest of the plan is only Java work.
1. **Seekable decoding in `alac2wav`.** Add `SeekableAlacDecoder`:
    - Open the file once (`FileChannel`), read the `alac` magic cookie from `stsd`, and build a packet index from
      `stsz` (packet sizes), `stco`/`co64` (chunk offsets), `stsc` (packets per chunk) and `stts` (frames per packet,
      normally 4096). Result: for packet *i* — file offset, size, first PCM frame.
    - `decodePacket(int i)` → PCM for that packet; `packetForFrame(long frame)` by binary search.
    - Unit tests: decoding all packets in order gives byte-for-byte the same PCM as today's full decoder.
2. **`AlacWavSource implements RandomAccessSource`** (app side): `length() = 44 + totalFrames × blockAlign`. It writes
   the WAV header itself; for `position ≥ 44` it maps bytes to frames, decodes the packets covering the requested
   range, and copies the bytes. It keeps a small LRU of decoded packets (8–16), so the player's sequential 4 KB reads
   decode each packet once. Thread-safe, since the native player may read from its own thread. Unit tests: random
   reads equal the same ranges of a fully decoded WAV.
3. **The patch module** `jfx-media-patch` (a Gradle subproject): the four classes above, compiled against
   `javafx-media-25.0.1` for Windows. Keep the JavaFX source of `Locator.java` next to the patched copy, so a diff
   shows the two edits; check it in. (JavaFX is GPLv2 with the Classpath Exception, so a modified copy of that file
   stays under that licence; fine for this app.)
4. **Getting the patch loaded — check this first.** It depends on how `javafx.media` is loaded at run time; log
   `Media.class.getModule().isNamed()` at start-up in each launch mode:
    - *Module path* (likely `gradle run`: the JavaFX plugin handles `controls` and `fxml`; `javafx-media` is added as a
      plain dependency, so check): add `--patch-module javafx.media=<path>/jfx-media-patch.jar` and
      `--add-exports javafx.media/com.sun.media.jfxmedia.locator=ALL-UNNAMED` to the run and test JVM arguments.
    - *Class path* (likely the deployed `java -jar cTunes.jar`, where Spring Boot puts every JavaFX jar in
      `BOOT-INF/lib`): put the patch classes into the app jar. Spring Boot searches `BOOT-INF/classes` before
      `BOOT-INF/lib`, so the patched `Locator` wins. `--patch-module` cannot go in a JAR manifest, so this route needs
      no launcher change.
    - Simplest is to make both modes the same (e.g. the class-path route for `gradle run` too).
5. **Wire it in:** `MediaProvider` creates an `AlacWavSource` for ALAC tracks, registers it in `StreamSources` and
   returns `new Media("file:///ctunes-stream/<trackId>.wav")`. `AudioPlayer` unregisters and closes the source when
   the player is disposed. MP3 (and AAC, should one appear) keeps using the real file URI.
6. **Fallback and self-check:** at start-up, check that the patch is active (`StreamSources` loads and `Locator` is
   the patched class, e.g. a marker constant). If not, log a warning and fall back to option A (the disk cache). That
   way a JavaFX upgrade or a wrong launch command degrades to "first play takes a second" instead of "ALAC doesn't
   play". Building option A as well is extra work; if you'd rather not, keep the current HTTP endpoint as the
   fallback instead and skip step 7. It already works and needs no internal API, it's just slower to seek.
7. **Remove the web layer** once 5–6 work (only if the fallback is option A, not the HTTP endpoint):
   `HttpRestController`, the HTTP branch of `MediaProvider`,
   `spring-boot-starter-web`, `spring-boot-starter-actuator`, `server.port` / `SERVER_PORT`, and set
   `spring.main.web-application-type: none` (item 8.1). Keep the H2 console in a `dev` profile only, if wanted.
8. **On every JavaFX upgrade:** diff the new `Locator.java` and `ConnectionHolder.java` against the stored originals,
   re-apply the two edits, run the spike test (step 0) and a seek test. Add a test that fails when the JavaFX version
   in `build.gradle` differs from the version the patch was made for, as a reminder.

**Bonus: the whole waveform at once.** With a seekable decoder, a background task can decode the whole track at low
priority (much faster than real time) and compute the 2000 waveform bars up front. The waveform bar then shows the
whole track from the first second instead of filling in as it plays. This is optional and independent of playback.

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

#### Thumbnails stored with the album

**Status: done** (step 6). Migration V007 adds the `album_cover_thumb` table (album, size, MD5 of the source cover,
JPEG) and `album.image_overridden`. `CoverThumbnails` makes the thumbnails and is tested. `AlbumCoverService`
supplies them to `CoverImages`, which decodes a 128 or 512 px thumbnail instead of the original wherever the album
is known and the cover is shown at up to 512 px. Instead of an `image_md5` column, the thumbnails store the MD5 of the
cover they were made from: the original bytes come with the album anyway, and its MD5 is cheap to compute on the
decoder thread.

The originals are already on disk (the `ALBUM.IMAGE` BLOB), so reading them is not the slow part: **decoding** a
full-size cover is (tens of milliseconds for a 1400 px JPEG, about 7.8 MB of pixels). So also store small,
pre-scaled thumbnails, and decode those instead.

The thumbnail is **the final image** wherever the cover is shown small, not a stand-in replaced by the real art
later. A queue row shows the cover at about 48 px and a grid card at 128–200 px. Decoding a 1400 px original for that
only to scale the pixels away is wasted work; a 128 or 512 px thumbnail looks the same at those sizes. A 128 px
thumbnail decodes in about a millisecond and takes about 64 KB of pixels.

- **Sizes:** 128 px (queue rows, album list, small grid cards) and 512 px (large grid cards, tag editor). The playback
  window's large art (up to 720 px, one cover at a time, already decoded in the background) keeps using the original.
- **Format:** JPEG, quality about 85 (covers have no transparency), typically 5–40 KB each. JavaFX cannot encode
  images, so scale with `Graphics2D` (bilinear or bicubic, in a couple of halving steps for large reductions) and
  encode with `ImageIO` (`java.desktop` module).
- **Storage:** a table next to the album rather than loose files, so thumbnails are backed up with the covers, can't be
  orphaned and need no cleanup:
  `album_cover_thumb(album_id INTEGER REFERENCES album(id) ON DELETE CASCADE, size SMALLINT, source_md5 CHAR(32), data BLOB, PRIMARY KEY (album_id, size))`.
- **When to update — tied to the cover, not to the audio files:** many tracks share one cover, and a cover can also
  change in the tag editor without any audio file changing. Store the MD5 of the original cover with each thumbnail.
  When an album's cover bytes change (rescan, item 6.5; or tag editor), the hash no longer matches and the thumbnails
  are regenerated, once.
- **Existing albums:** generate lazily the first time an album is shown (placeholder first, in the background), or in a
  one-off background job after the upgrade (a `BackgroundJob`, item 7.1). The placeholder only ever shows once per
  album, before its thumbnail exists.
- **Plus the in-memory cache above:** thumbnails make each decode cheap; the memory cache stops repeating it. Each
  `Image` is decoded once per album and size.
- **Disk use:** a few KB per album, written once.

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

**How:** use the async framework from the sibling project (item 7.1): a `CancellationToken` per kind of load, and a
`Flow` per load. Choosing an artist takes `cancelAndSnapshot()`, which invalidates the load still in flight. Its
remaining steps then stop silently, and only the newest result reaches the screen. That replaces the hand-written
`albumsRequestId` check.

```java
private final CancellationToken albumsLoad = new CancellationToken();

private void loadArtistAlbums(ArtistRecord artist) {
    CancellationToken.Snapshot load = albumsLoad.cancelAndSnapshot();
    showAlbumsLoading();
    Flow.begin()
        .cancelledBy(load)
        .bg(() -> buildAlbums(artist, load))      // one query (2.2); checks load.isValid() before each album
        .fx(this::showAlbums)                     // skipped if another artist was chosen meanwhile
        .onError(e -> showAlbumsError(artist, e));
}
```

`Flow` checks the token only **between** steps. `buildAlbums` is one long step (it builds every album), so it should
check `load.isValid()` itself before each album and stop early; otherwise a superseded load still runs to the end
(its result is just thrown away). See 7.2 for a small framework addition that makes this check easier.

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

**How:** publish progress at most every 100 ms or every 1 %, and always at the end. Log each file at DEBUG. Moving the
scan to a `BackgroundJob` (item 7.1) gives this for free: `publishProgressThrottled` already does it.

### 5.4 The stop flag is not `volatile`

**Where:** `private boolean shouldInterrupt;`, written by the event listener thread and read by the `rescan` thread.

**Why:** without `volatile` (or another happens-before edge), the Java memory model allows the scan thread to never see
the change. The JIT may even lift the read out of the loop, so **Stop can be ignored**.

**How:** `private volatile boolean shouldInterrupt;` (or an `AtomicBoolean`). Moving the scan to a `BackgroundJob`
(item 7.1) also fixes it: the job's status field is `volatile`, and the loop polls `isInterruptRequested()`.

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

### 6.5 A rescan never updates an existing album's cover

**Status: done** (step 6). The first file of an album read in a scan decides its cover, usually its first track,
since files are visited in name order. A cover set in the tag editor sets `album.image_overridden` and is kept, and
a file without a cover keeps the current one.

**Where:** `DataAccess.getOrInsertAlbum(artistId, album, image)`: when the album already exists, it returns it as it
is and ignores `image`.

**Why:** if the artwork is replaced in the audio files (for example after fixing it in iTunes) and the library is
rescanned, the new cover never reaches the database. The app keeps showing the old one until the album is deleted or
the cover is changed in the tag editor. With thumbnails (2.1) the rescan is meant to be one of the ways a cover
gets updated, so this has to be fixed first.

**How:** compute the MD5 of the file's embedded cover (store it in an `ALBUM.IMAGE_MD5` column, migration `V006`). On
rescan, when the album exists and the hash differs, update `ALBUM.IMAGE` and the hash; the thumbnails are then
regenerated (2.1). Decide which cover wins when the tracks of one album embed different covers. Simplest: the first
track's (lowest disc and track number); or keep the current cover unless *every* track has changed. Skip the update
when the cover was set by hand in the tag editor, since that's an override (check `TRACK_OVERRIDES_HISTORY` or add an
`ALBUM.IMAGE_OVERRIDDEN` flag).

---

## 7. Threads

### 7.1 About ten raw `new Thread(...)` call sites → adopt the async framework

**Where:** `LibraryController` (2), `SearchController`, `LibraryLyricsTabController`, `LibraryHistoryTabController`,
`LyricsService` (2), `FilesScanningService`, `PlaybackWindow` — only some call `setDaemon(true)`. Several also hand-roll
"is this result still wanted?" checks (`albumsRequestId`, the history tab's request ids, `paletteToken`).

**Why:** no limit (a burst of clicks gives a burst of threads), no cancellation, exceptions go to the default handler
and are easy to miss, and a non-daemon thread still running keeps the JVM alive after the window closes.

**How:** use the async framework from the sibling project (`util.async`: `Flow`, `CancellationToken`, `DelayedAction`,
and `jobs`: `JobManager`, `BackgroundJob`). It covers both kinds of background work this app has:

- **`Flow` + `CancellationToken`** for "load in the background, show on the UI": `bg(...)` steps run on virtual threads,
  `fx(...)` steps on the JavaFX thread. A token makes superseded loads stop silently, and `onError` sees only real
  failures.
- **`JobManager` + `BackgroundJob`** for long jobs with progress and a Stop button: one worker thread, a queue,
  duplicate
  suppression, and cooperative interruption. It has throttled progress built in (`publishProgressThrottled`).

| Call site                                                   | Today                                | With the framework                                                               |
|-------------------------------------------------------------|--------------------------------------|----------------------------------------------------------------------------------|
| Artist selected → albums (`LibraryController`)              | `new Thread` + `albumsRequestId`     | `Flow` + token `albumsLoad` (2.3)                                                |
| Library updated → artists (`onLibraryDataUpdated`)          | `new Thread`                         | `Flow` + token `artistsLoad`                                                     |
| Search (`SearchController`)                                 | debounce + `new Thread` + request id | keep the `PauseTransition` debounce; `Flow` + token `search`                     |
| Lyrics tab load (`LibraryLyricsTabController`)              | `new Thread`                         | `Flow` + token `lyricsLoad`                                                      |
| History tab (`LibraryHistoryTabController.runInBackground`) | own thread helper + ids              | `Flow` + token per tab (period changes cancel the previous load)                 |
| Lyrics fetching (`LyricsService`)                           | `new Thread` (×2)                    | `Flow` (one-off fetch); a `BackgroundJob` if it ever runs over the whole library |
| Album art palette (`PlaybackWindow.loadPalette`)            | daemon thread + `paletteToken`       | `Flow` + token `paletteLoad`                                                     |
| Cover / thumbnail decoding (2.1)                            | on the JavaFX thread                 | `Flow`; a cell or card that is reused cancels its own previous load              |
| Library rescan (`FilesScanningService`)                     | `new Thread` + non-volatile flag     | `BackgroundJob` — also fixes 5.3 (throttled progress) and 5.4 (volatile status)  |
| Thumbnail backfill (2.1), whole-track waveform (section 1)  | —                                    | `BackgroundJob`                                                                  |
| Debounced saves                                             | `DelayedAction` (already a copy)     | the framework's `DelayedAction` (same class, with named threads)                 |

**Sharing the code:** the two projects already have diverging copies of `DelayedAction`. Rather than copying the
framework a second time, publish it as a small library, the same way as `alac2wav`
(`com.github.curious-odd-man:curious-async`), or include it as a Gradle composite build (`includeBuild`) while it's
still changing. Both apps then get fixes at once.

**Virtual threads and the database:** `Flow` runs `bg` steps on a virtual-thread-per-task executor. On Java 24+ (this
app is on 25), `synchronized` blocks no longer pin virtual threads (JEP 491), so JDBC and H2 are fine there. The
number of concurrent queries is still bounded by the connection pool (Hikari, 10 by default).

### 7.2 Changes the async framework needs before it moves over

**Status: done** (step 5). The framework is now in this project, as `util/async` (`Flow`, `CancellationToken`,
`DelayedAction`) and `util/async/jobs` (`JobManager`, `BackgroundJob`, `JobEventPublisher`, …), with all seven
changes below. The jobs part is covered by `JobManagerTest`. It still needs moving into a shared library, so the two
projects don't keep separate copies. The call sites in 7.1 are moved over, except the cover decoder in
`CoverImages`: it keeps its own pool of two threads on purpose, because virtual threads would let a burst of new rows
start hundreds of decodes at once.

Found while reviewing it for this app; worth fixing in the shared library so both apps benefit.

1. **Failures are lost when no `onError` is attached.** `Step.run()` returns the raw future; if a step throws and the
   caller didn't attach `onError`, nothing is logged. *Fix:* in `run()`, attach a handler that logs real failures
   (not `CancellationException`), so forgetting `onError` can't hide a bug.
2. **Cancellation is only checked between steps.** A long `bg` step (building every album, scanning a list) runs to
   the end after it was superseded. *Fix:* let a step check its own token, e.g. a `bg(Function<Snapshot, T>)` variant
   that passes the snapshot in, or a public `flow.checkCancelled()` that throws `CancellationException` (already
   treated as silent).
3. **`JobManager.close()` can hang shutdown.** It spins on `Thread.yield()` until the current job stops; a job that
   doesn't poll `isInterruptRequested()` keeps the app from exiting and burns a CPU core meanwhile. *Fix:* wait with a
   timeout (e.g. 5 s, sleeping rather than yielding), then go on. Also make the worker a daemon thread, so a stuck job
   can never keep the JVM alive.
4. **A failed job doesn't tell the UI.** `BackgroundJob.run` catches the exception, logs it and sets `FAILED`, but
   doesn't call `publishFailed(e)`, so a progress bar would stay "running". Interruption is the same: the job only
   reports it if `runImpl` calls `publishInterrupted()` itself. *Fix:* publish both from `run()`.
5. **The jobs are tied to the other app's events.** `BackgroundJob` publishes `curious_designer` event payload classes;
   curiousTunes has its own flat `BackgroundProcessEvent` (process name, description, progress, max, error, type).
   *Fix:* make the library event-agnostic: a small `JobEventPublisher` interface (`started`, `progress`, `ended`,
   `interrupted`, `failed`) that each app implements with its own events.
6. **`JobFactory`** only holds the other app's `TimeProvider`: leave it out of the library.
7. **`DelayedAction`** isn't thread-safe (`pendingSave` is a plain field) and starts one platform thread per instance.
   That's fine while it's only used from the JavaFX thread; say so in its Javadoc, or make `reSchedule` `synchronized`
   and share one scheduler between instances.

---

## 8. Start-up and logging

### 8.1 Web server and Actuator only for the audio endpoint

**Where:** `build.gradle`: `spring-boot-starter-web`, `spring-boot-starter-actuator`; `application.yaml`: `server.port`,
`web-application-type: servlet`.

**Why:** embedded Tomcat, Spring MVC and Actuator add start-up time (typically a second or more) and tens of MB of
memory, and open a network port (see 1.3), all for one internal endpoint.

**How:** after option G (section 1), remove both starters and set `spring.main.web-application-type: none`. Keep the H2
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
3. **m4a — option G** (about a week): spike first (step 0), then the seekable decoder in `alac2wav`, the virtual WAV
   source, the JavaFX patch, wiring and fallback, and finally removing the web layer (8.1). Every `.m4a` in the
   library is ALAC, so this gives instant starts and exact seeking for most of what you play. Add the 1.1 codec
   column in the same migration as a safety net.
4. **Scan speed**: 5.1 skip unchanged files, 5.2 batched transactions, 5.3 throttled progress, 6.3 index (one `V006`
   migration can carry the columns for 1.1 and 5.1 and the index).
5. **Async framework** (a day or two): extract it as a shared library with the 7.2 fixes, then move the call sites in
   7.1 over, starting with the artist view (2.3) and the rescan (which also settles 5.3 and 5.4).
6. **Larger UI work**: 2.1 thumbnails with 6.5 (cover updates on rescan), 2.2, 2.4, 2.5 artist view, then 3.1
   virtualised queue.
