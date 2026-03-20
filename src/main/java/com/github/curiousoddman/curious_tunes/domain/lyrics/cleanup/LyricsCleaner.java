package com.github.curiousoddman.curious_tunes.domain.lyrics.cleanup;

import com.github.curiousoddman.curious_tunes.domain.lyrics.cleanup.flagger.Flag;
import com.github.curiousoddman.curious_tunes.domain.lyrics.cleanup.flagger.SuspicionFlagger;
import com.github.curiousoddman.curious_tunes.domain.lyrics.cleanup.step.CleaningStep;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

// AI Generated
@Service
@RequiredArgsConstructor
public class LyricsCleaner {
    private final List<CleaningStep> steps;
    private final List<SuspicionFlagger> flaggers;

    public CleanerResult clean(String lyrics) {
        List<Flag> flags = flaggers.stream()
                .filter(f -> f.test(lyrics))
                .map(SuspicionFlagger::flag)
                .toList();

        // broken songs are returned as-is — cleaning could corrupt the artifact evidence
        if (flags.contains(Flag.PARSING_ERROR)) {
            return new CleanerResult(lyrics, lyrics, flags);
        }

        if (lyrics == null || lyrics.isBlank()) {
            return new CleanerResult(lyrics, null, flags);
        }

        List<String> result = Arrays.asList(lyrics.split("\n"));
        for (CleaningStep step : steps) {
            result = step.apply(result);
        }

        return new CleanerResult(lyrics, Strings.join(result, '\n'), flags);
    }
}