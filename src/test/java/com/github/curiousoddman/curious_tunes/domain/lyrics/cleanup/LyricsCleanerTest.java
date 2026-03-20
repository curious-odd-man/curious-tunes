package com.github.curiousoddman.curious_tunes.domain.lyrics.cleanup;

import com.github.curiousoddman.curious_tunes.domain.lyrics.cleanup.flagger.BoxSizeOfZeroMeansFlagger;
import com.github.curiousoddman.curious_tunes.domain.lyrics.cleanup.flagger.Flag;
import com.github.curiousoddman.curious_tunes.domain.lyrics.cleanup.step.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LyricsCleanerTest {


    // -------------------------------------------------------------------------
    // TrailingWhitespaceStep
    // -------------------------------------------------------------------------

    @Test
    void trailingWhitespace_stripsSpacesFromEachLine() {
        var step = new TrimEachLineStep();
        assertThat(step.apply(l("hello   \nworld  \n(fade)          ")))
                .isEqualTo(l("hello\nworld\n(fade)"));
    }

    // -------------------------------------------------------------------------
    // FooterStripStep
    // -------------------------------------------------------------------------

    @Test
    void footerStrip_removesFullFooterBlock() {
        var step = new FooterStripStep();
        String input = "Some lyrics line\n\n(fade)\nExplain\nRequest\n×";
        assertThat(step.apply(l(input))).isEqualTo(l("Some lyrics line"));
    }

    @Test
    void footerStrip_removesPartialFooter() {
        var step = new FooterStripStep();
        String input = "Some lyrics line\nExplain\nRequest\n×";
        assertThat(step.apply(l(input))).isEqualTo(l("Some lyrics line"));
    }

    @Test
    void footerStrip_leavesCleanLyricsUntouched() {
        var step = new FooterStripStep();
        String input = "Line one\nLine two\nLine three";
        assertThat(step.apply(l(input))).isEqualTo(l(input));
    }

    // -------------------------------------------------------------------------
    // BandNameFooterStep
    // -------------------------------------------------------------------------

    @Test
    void bandNameFooter_removesBandNameLine() {
        var step = new BandNameFooterStep();
        assertThat(step.apply(l("Some lyrics\nDREAM THEATER LYRICS")))
                .isEqualTo(l("Some lyrics"));
    }

    @Test
    void bandNameFooter_removesBandNameWithTrailingBlanks() {
        var step = new BandNameFooterStep();
        assertThat(step.apply(l("Some lyrics\nDIO LYRICS\n\n")))
                .isEqualTo(l("Some lyrics\n\n"));
    }

    @Test
    void bandNameFooter_leavesNormalLastLineUntouched() {
        var step = new BandNameFooterStep();
        String input = "Some lyrics\nI could never let you go.";
        assertThat(step.apply(l(input))).isEqualTo(l(input));
    }

    // -------------------------------------------------------------------------
    // CollapseBlankLinesStep
    // -------------------------------------------------------------------------

    @Test
    void collapseBlankLines_collapsesTwoConsecutiveBlanks() {
        var step = new CollapseBlankLinesStep();
        assertThat(step.apply(l("Line one\n\n\nLine two")))
                .isEqualTo(l("Line one\n\nLine two"));
    }

    @Test
    void collapseBlankLines_stripsLeadingAndTrailingBlanks() {
        var step = new CollapseBlankLinesStep();
        assertThat(step.apply(l("\n\nLine one\nLine two\n\n")))
                .isEqualTo(l("Line one\nLine two"));
    }

    @Test
    void collapseBlankLines_preservesSingleBlankBetweenVerses() {
        var step = new CollapseBlankLinesStep();
        String input = "Verse one\n\nVerse two";
        assertThat(step.apply(l(input))).isEqualTo(l(input));
    }

    // -------------------------------------------------------------------------
    // GluedLineStep
    // -------------------------------------------------------------------------

    @Test
    void gluedLine_splitsLatinGluedLines() {
        var step = new GluedLineStep();
        assertThat(step.apply(l("And I looked into your eyesCaptivated by your smile")))
                .isEqualTo(l("And I looked into your eyes\nCaptivated by your smile"));
    }

    @Test
    void gluedLine_splitsCyrillicGluedLines() {
        var step = new GluedLineStep();
        assertThat(step.apply(l("привет мирКак дела")))
                .isEqualTo(l("привет мир\nКак дела"));
    }

    @Test
    void gluedLine_leavesNormalLinesUntouched() {
        var step = new GluedLineStep();
        String input = "I'm feeling like a Monday";
        assertThat(step.apply(l(input))).isEqualTo(l(input));
    }

    // -------------------------------------------------------------------------
    // BrokenSongFlagger
    // -------------------------------------------------------------------------

    @Test
    void brokenSongFlagger_detectsArtifactText() {
        var flagger = new BoxSizeOfZeroMeansFlagger();
        assertThat(flagger.test("box size of zero means 'till end of file")).isTrue();
    }

    @Test
    void brokenSongFlagger_passesNormalLyrics() {
        var flagger = new BoxSizeOfZeroMeansFlagger();
        assertThat(flagger.test("Mamma mia, here I go again")).isFalse();
    }

    // -------------------------------------------------------------------------
    // Full pipeline (no Spring context — wired manually)
    // -------------------------------------------------------------------------

    @Test
    void fullPipeline_cleansRealWorldExample() {
        var cleaner = new LyricsCleaner(
                List.of(
                        new TrimEachLineStep(),
                        new FooterStripStep(),
                        new BandNameFooterStep(),
                        new CollapseBlankLinesStep(),
                        new GluedLineStep()
                ),
                List.of(
                        new BoxSizeOfZeroMeansFlagger()
                )
        );

        String input = """
                Oooh, I wanna share it with you   \s
                
                
                (fade)                   \s
                Explain
                Request
                
                
                ×""";

        CleanerResult result = cleaner.clean(input);
        assertThat(result.cleaned()).isEqualTo("Oooh, I wanna share it with you");
        assertThat(result.hasFlags()).isFalse();
        assertThat(result.wasModified()).isTrue();
    }

    @Test
    void fullPipeline_flagsBrokenSongAndSkipsCleaning() {
        var cleaner = new LyricsCleaner(
                List.of(new TrimEachLineStep()),
                List.of(new BoxSizeOfZeroMeansFlagger())
        );

        String input = "box size of zero means 'till end of file. That is not yet supported";
        CleanerResult result = cleaner.clean(input);

        assertThat(result.flags()).contains(Flag.PARSING_ERROR);
        assertThat(result.cleaned()).isEqualTo(input); // untouched
    }

    @Test
    void fullPipeline_flagsDanglingLabelButStillCleans() {
        var cleaner = new LyricsCleaner(
                List.of(
                        new TrimEachLineStep(),
                        new FooterStripStep(),
                        new CollapseBlankLinesStep()
                ),
                List.of(new BoxSizeOfZeroMeansFlagger())
        );

        String input = "Some lyrics\nRequest\n×\n";
        CleanerResult result = cleaner.clean(input);

        assertThat(result.cleaned()).isEqualTo("Some lyrics");
    }

    private static List<String> l(String s) {
        return Arrays.asList(s.split("\n"));
    }
}
