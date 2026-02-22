package com.github.curiousoddman.curious_tunes.util.async;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "waveform-writer", havingValue = "async")
public class AsyncWaveformWriter implements WaveformWriter {
    private AsyncFileWriter asyncFileWriter;
    private int fileIndex = 0;

    @SneakyThrows
    @Override
    public void append(double timestamp,
                       double duration,
                       float[] magnitudes,
                       float[] phases,
                       float db,
                       double wi) {
        if (asyncFileWriter == null) {
            asyncFileWriter = new AsyncFileWriter(fileIndex++ + ".csv", false);
            String mColumns = IntStream.rangeClosed(1, magnitudes.length)
                    .mapToObj(i -> "m" + i)
                    .collect(joining(","));
            String pColumns = IntStream.rangeClosed(1, phases.length)
                    .mapToObj(i -> "p" + i)
                    .collect(joining(","));
            asyncFileWriter.writeLine("ts,dur," + mColumns + "," + pColumns + ",db,wi");
        }
        String sb = String.valueOf(timestamp) + ',' +
                duration + ',' +
                Arrays.toString(magnitudes).replaceAll("[\\[]]", "") + ',' +
                Arrays.toString(phases).replaceAll("[\\[]]", "")
                + "," + db + "," + wi;

        asyncFileWriter.writeLine(sb);
    }

    @Override
    public void stop() {
        if (asyncFileWriter != null) {
            asyncFileWriter.close();
            asyncFileWriter = null;
        }
    }
}
