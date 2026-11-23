# Visualiser sync calibration and player priority

Two related changes: a manual test that lets the user line the spectrum visualiser up with what they hear, and a
higher Windows process priority so playback stutters less.

## 1. Background

The spectrum visualiser in the playback window (see [playback-window.md](playback-window.md), decision 19) is drawn
from the spectrum JavaFX reports while decoding. The sound reaches the speakers later, after the audio output's
buffers (and, with Bluetooth headphones, the headset's own buffer). The visualiser is therefore drawn a fixed delay
after the spectrum arrives. The right delay depends on the user's sound card, driver and output device, and JavaFX
cannot report it, so the user sets it by eye and ear.

## 2. Visualiser sync calibration

### 2.1 Requirements

| ID   | Requirement                                                                                                                                                                                                                               |
|------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| C-1  | The delay between the spectrum arriving and the visualiser drawing it ("visualiser delay") is a user setting in milliseconds, 0–500, default 150. It is saved in the user preferences and applies from the next frame drawn.              |
| C-2  | A calibration window opens from **File › Calibrate visualiser…** in the library window, and from the right-click menu of the visualiser in the playback window.                                                                           |
| C-3  | The window plays a generated click track: one click per second, every fourth click louder. Each click is a short noise burst plus a short low tone, so the whole spectrum, bass to treble, jumps at once.                                 |
| C-4  | The click track plays through a JavaFX `MediaPlayer`, like a song, so it goes through the same audio output path and has the same delay as music. It loops until the window is closed.                                                    |
| C-5  | The window shows the real visualiser (same bars, same timing code as the playback window) and a large flash that lights up on each click, both drawn with the delay being set.                                                            |
| C-6  | A slider sets the delay (0–500 ms, steps of 5 ms) and takes effect immediately while the clicks keep playing. Buttons −10 ms and +10 ms adjust it finely; the current value is shown in ms.                                               |
| C-7  | **Save** stores the value and closes. **Cancel** (or closing the window) closes without changing the setting. **Reset** sets the slider back to the default (150 ms) without saving.                                                      |
| C-8  | The window explains in two short lines how to use it: adjust until the flash lands on the click; if unsure, choose slightly higher (a picture a little late is less noticeable than early); re-run after changing headphones or speakers. |
| C-9  | If music is playing when the window opens, it is paused, and resumed when the window closes. If it was paused or stopped, it stays so.                                                                                                    |
| C-10 | The generated audio is written to a temporary file, deleted when the window closes (and on exit, should the window not close normally).                                                                                                   |
| C-11 | If the click track cannot play (no audio device, decoder error), the window says so instead of showing a silent test.                                                                                                                     |

### 2.2 Out of scope

- Automatic measurement of the delay (comparing spectrum timestamps with the player's clock). Can be added later; the
  slider would then become a correction on top of it.
- Separate values per output device. JavaFX cannot tell which device is in use; the user re-runs the calibration after
  switching.
- Applying the delay to the moving playhead of the waveform, which follows the player's own clock.

## 3. Player priority on Windows

### 3.1 Requirements

| ID  | Requirement                                                                                                                                                                                            |
|-----|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| P-1 | On Windows, the application raises its own process priority class to **Above normal** as early as possible at start-up. This applies to all its threads, including the native audio threads of JavaFX. |
| P-2 | **High** is available but not the default: it can starve other programs. **Realtime** is not offered (it needs administrator rights and can freeze the system).                                        |
| P-3 | The level can be chosen with the system property `ctunes.priority` = `normal`, `above_normal` (default) or `high`, e.g. `-Dctunes.priority=high`.                                                      |
| P-4 | It needs no administrator rights, no extra library and no helper process: the Windows API is called directly through Java's foreign function API.                                                      |
| P-5 | On other operating systems, or if the call fails, the application starts normally and logs why the priority was not changed.                                                                           |
| P-6 | The log states the priority that was set.                                                                                                                                                              |

### 3.2 Notes

- The call is restricted native access; the JVM needs `--enable-native-access=ALL-UNNAMED` to run it without a warning.
- A higher priority helps when other programs compete for the CPU. Stutter caused inside the application (long work on
  the JavaFX thread, long garbage-collection pauses) is not fixed by it.
