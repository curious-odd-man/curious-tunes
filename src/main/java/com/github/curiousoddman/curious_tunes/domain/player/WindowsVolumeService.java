package com.github.curiousoddman.curious_tunes.domain.player;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;


@Slf4j
@Component
public class WindowsVolumeService {

    public float getMasterVolume() {
        return runScript("get_master_volume.ps1");
    }

    public double getEffectiveVolume(double mediaPlayerVolume) {
        return mediaPlayerVolume * getMasterVolume();
    }

    // ── Script runner ─────────────────────────────────────────────────────────

    private float runScript(String scriptName) {
        Path scriptFile = null;
        try {
            scriptFile = extractScript(scriptName);

            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe", "-NoProfile", "-NonInteractive",
                    "-ExecutionPolicy", "Bypass",
                    "-File", scriptFile.toAbsolutePath().toString());
            pb.redirectErrorStream(true);

            Process p = pb.start();
            String out = new String(p.getInputStream().readAllBytes()).trim();
            boolean finished = p.waitFor(5, TimeUnit.SECONDS);

            if (!finished) {
                p.destroyForcibly();
                log.warn("{} timed out", scriptName);
                return 1.0f;
            }

            // Take the last non-empty line as the result
            String lastLine = Arrays.stream(out.split("\\R"))
                    .map(String::trim)
                    .filter(l -> !l.isEmpty())
                    .reduce("", (a, b) -> b);

            return Float.parseFloat(lastLine);

        } catch (Exception e) {
            log.warn("{} failed: {}", scriptName, e.getMessage());
            return 1.0f;
        } finally {
            if (scriptFile != null) {
                try {
                    Files.deleteIfExists(scriptFile);
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * Copies the script from classpath resources to a temp file so
     * PowerShell can execute it via -File (requires a real path).
     */
    private Path extractScript(String scriptName) throws IOException {
        String resourcePath = "/scripts/" + scriptName;
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Script not found on classpath: " + resourcePath);
            }
            Path tmp = Files.createTempFile(
                    scriptName.replace(".ps1", "_"), ".ps1");
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            return tmp;
        }
    }
}