# IntelliJ inspection review

The results of an IntelliJ inspection run (866 findings), reviewed one by one, and what was done about each. Fixed means
changed in the code. Left means left on purpose, with the reason. Open means it's your call (see the end).

## Fixed

| Inspection                                                 | Where                                                                                            | What                                                                                                                                                                                                                                                             |
|------------------------------------------------------------|--------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| CodeBlock2Expr (the real issue was a leak)                 | `PlaylistModel.addItems`                                                                         | Each call added another selection listener that was never removed. After N adds, every selection change dumped the whole queue N times to the debug log (which is on for `model.playlist` in dev). Removed the listener; the other playlist changes already log. |
| DataFlowIssue (9)                                          | `JavafxApplication`, `AnimatedPreloader`, `PlaybackWindow`                                       | `new Image(getResourceAsStream(..))`: an NPE deep inside `Image` if a file is missing, and the streams were never closed. New `util/AppResources` loads by URL and names the missing file. The four icon lines that appeared in two places are now one call.     |
| ConstantValue                                              | `ExceptionTranslator`                                                                            | `ctx.exception()` is a `RuntimeException` in jOOQ, so the wrapping branch only ever ran with a `null` cause. Simplified.                                                                                                                                         |
| Deprecation                                                | `LibraryHistoryTabController`                                                                    | `CONSTRAINED_RESIZE_POLICY` → `CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN`, the replacement JavaFX names. Check in manual test 2b.                                                                                                                               |
| StringConcatenationArgumentToLogCall (3)                   | `MediaPlayerListeningTracker`                                                                    | `String.format` ran on every play even with INFO off. Now `{}` placeholders.                                                                                                                                                                                     |
| MathClampMigration (18 of 26)                              | Several                                                                                          | `Math.max(lo, Math.min(hi, x))` → `Math.clamp(x, lo, hi)` where both bounds are constants.                                                                                                                                                                       |
| MismatchedCollectionQueryUpdate                            | `LibraryArtistAlbumController.discControllers`                                                   | A list that was filled and never read. Removed.                                                                                                                                                                                                                  |
| unused (my code, dead since the queue became a `ListView`) | `PlaylistItemController.dispose`, `LibraryAlbumTrackRow.getTrackInfo`                            | Removed. Cells are reused now, so `dispose` had no caller and no leak to prevent.                                                                                                                                                                                |
| unused / FieldCanBeLocal                                   | `PlaybackWindow.headerBar`, `LibraryController` `artistId`                                       | Written, never read. Removed.                                                                                                                                                                                                                                    |
| UNUSED_IMPORT (3), RedundantCast, UnnecessaryToStringCall  | `FooterStripStep`, `MetadataTagsBase`, `WaveformWriter`, `Accessibility`, `FilesScanningService` | Trivial.                                                                                                                                                                                                                                                         |
| DuplicateExpressions, RedundantThrows                      | `MediaProvider`, `HttpRestController`                                                            | Gone with the web layer.                                                                                                                                                                                                                                         |
| DuplicatedCode (8)                                         | `LibraryAlbumTrackRow` vs `LibraryArtistAlbumTrackController`                                    | Already resolved: the second class was deleted in step 6. The inspection ran on an older copy.                                                                                                                                                                   |

## Left as they are, on purpose

- **CSS (CssUnknownProperty 140, CssInvalidPseudoSelector 19, CssInvalidFunction 3): false positives.** IntelliJ
  checks the files as web CSS. `-fx-*`, `derive()`, `:filled`, `:focused`, `:selected`, `:odd` and `:even` are all
  valid JavaFX CSS. These go away if the inspection profile turns the CSS inspections off for
  `src/main/resources/styles`.
- **CssUnusedSymbol (95), JavaFxUnresolvedStyleClassReference (83): mostly false positives.** Style classes are added
  from Java (`getStyleClass().add("pw-…")`, cells, `ikonli-font-icon`), and stylesheets are attached at run time, so
  IntelliJ can't link FXML to CSS. Some may really be dead selectors, but sorting them out means checking each one in
  the
  running app. Not worth it blind.
- **JavaFxDefaultTag (85), JavaFxRedundantPropertyValue (14):** that's how Scene Builder writes FXML (`<children>`,
  default values). Removing them is churn that Scene Builder puts back on the next save.
- **JavaFxUnresolvedFxIdReference (2):** `contentsVbox` and `playModeGroup` have no controller field. `playModeGroup`
  is used inside the FXML (`$playModeGroup`), and an extra `fx:id` is harmless.
- **XmlHighlighting (36), XmlUnusedNamespaceDeclaration (2):** Inkscape metadata in the two SVG source files. Harmless.
- **Everything in `src/jfxPatch` (Locator: CanBeFinal, CatchMayIgnoreException, DuplicateThrows, ConstantValue, …):**
  this is JavaFX's own file with two guarded edits. Keeping it identical to the original otherwise makes the diff
  against the next JavaFX version trivial.
- **Everything in `CustomizedPropertyBoxParser`:** a copy of mp4parser code (Apache 2.0, Sebastian Annies). Same
  reasoning.
- **unused: "not reachable from the entry points", "class is not instantiated" (about 100):** false positives. These
  are Spring beans, FXML handlers and JavaFX callbacks (`AlacStreaming`, `UserPreferencesService`, `PlaybackWindow`, …).
  IntelliJ didn't treat Spring components as entry points in this run. `LyricsHelper` "never used" is wrong too: three
  lyrics sources call it.
- **Parameter `actionEvent` / `mouseEvent` is not used (46):** FXML handler signatures. Harmless, and the usual style.
- **MathClampMigration (8 left):** where a bound is computed (for example `Math.min(leftWidth - 40, …)`, `size() - 1`).
  `Math.clamp` throws if the upper bound ends up below the lower one, where the old code just returned the lower bound.
  So a narrow window or an empty list would turn into an exception.
- **BusyWait** in `JobManager.close`: a 20 ms poll, for at most 5 s, only at shutdown. A wait/notify would add code
  for no gain.
- **NullableProblems (10):** overrides of Spring methods annotated with JSpecify. The code is right. Silencing it means
  `@NullMarked` packages, a larger decision.
- **Style suggestions** (LombokGetterMayBeUsed, ClassCanBeRecord, BooleanMethodIsAlwaysInverted, IfCanBeSwitch,
  DeconstructionCanBeUsed, ExtractMethodRecommender, SameParameterValue, SameReturnValue, UnusedReturnValue,
  UnnecessaryModifier on `main`, …): matters of taste, or deliberate API (the `Flow` / job framework is a shared
  library). `public` on `main` stays for launchers that look it up by reflection.
- **SqlSourceToSinkFlow** in `ExceptionTranslator`: false positive. The SQL string is only passed to Spring's error
  translator for its message; nothing runs it.

## Your decisions, done

1. **Unused code removed.**
    - `DataAccess`: the five unused queries.
    - `PlaylistModel.deleteSelected` and `getNextForPlayback`.
    - The unused `TrackInfo` delegates.
    - `TrackStatus` is down to `ACTIVE`, the only value ever written.
    - `UserPrefKey.LAST_PLAYLIST_ID`, `UiIcons.SKIP_PREVIOUS`, `SwitchShuffleEvent` and the two-argument
      `RescanLibraryEvent` constructor.
    - The commented-out "bulk fix lyrics" startup block: a rescan with *Read every file again* does the same.
    - The tag classes are now read-only. The setters, the `on…Updated` hooks and `updateFile` all go: writing was
      already switched off and had no caller. The tag objects no longer keep the parsed MP4 box tree or the `Mp3File`
      after reading.
    - Kept: the `BackgroundProcessEvent` constructor. Lombok's `@Builder` calls it, so that finding was a false
      positive.
2. **The durable-actions feature is removed.**
    - The `retryable` package and its test.
    - The startup task that queried `pending_action` on every launch.
    - The table itself (migration `V008__drop_pending_action.sql`) and its generated jOOQ classes (edited by hand,
      like V006; a `gradle jooqCodegen` run gives the same result).
    - Jackson, which only these classes and LyricsnMusic used.
3. **Lyrics sources:**
    - LyricsnMusic is removed.
    - LyricsMode now uses HTTPS.
    - DarkLyrics stays on HTTP (it has no HTTPS), with a comment saying so.
