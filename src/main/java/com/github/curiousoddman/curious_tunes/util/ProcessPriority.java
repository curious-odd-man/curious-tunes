package com.github.curiousoddman.curious_tunes.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.Locale;
import java.util.Optional;

/**
 * Raises the priority of the application's process on Windows, so playback stutters less when other programs keep
 * the CPU busy (doc/visualiser-calibration.md, section 3). The priority class applies to every thread of the
 * process, including the native audio threads of JavaFX.
 * <p>
 * Calls the Windows API directly through the foreign function API: no extra library, no helper process,
 * no administrator rights. The JVM needs native access enabled for the unnamed module (the JAR manifest's
 * {@code Enable-Native-Access}, or {@code --enable-native-access=ALL-UNNAMED}), otherwise it warns on the first call.
 */
@Slf4j
public final class ProcessPriority {
    /**
     * System property choosing the level: {@code normal}, {@code above_normal} (default) or {@code high}.
     */
    public static final String PROPERTY = "ctunes.priority";

    @RequiredArgsConstructor
    public enum Level {
        NORMAL(0x0000_0020, "Normal"),
        ABOVE_NORMAL(0x0000_8000, "Above normal"),
        /**
         * Can starve other programs; only on request. Realtime is not offered: it needs administrator rights and
         * can freeze the system.
         */
        HIGH(0x0000_0080, "High");

        private final int    windowsClass;
        private final String displayName;
    }

    private ProcessPriority() {
    }

    /**
     * Sets the priority chosen by {@link #PROPERTY} (default {@link Level#ABOVE_NORMAL}). Does nothing on other
     * operating systems. Never throws: if the priority cannot be set, the reason is logged and the application
     * goes on at normal priority.
     */
    public static void apply() {
        String os = System.getProperty("os.name", "");
        if (!os.toLowerCase(Locale.ROOT)
               .startsWith("windows")) {
            log.info("Process priority left unchanged: not Windows ({})", os);
            return;
        }
        String property = System.getProperty(PROPERTY);
        Level  level    = parse(property).orElse(Level.ABOVE_NORMAL);
        if (property != null && parse(property).isEmpty()) {
            log.warn("Unknown {}='{}' (use normal, above_normal or high); using {}", PROPERTY, property, level.displayName);
        }
        try {
            setPriorityClass(level);
        } catch (Throwable e) {
            log.warn("Could not set the process priority to {}; running at normal priority", level.displayName, e);
        }
    }

    static Optional<Level> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String key = value.trim()
                          .toUpperCase(Locale.ROOT)
                          .replace('-', '_')
                          .replace(' ', '_');
        if (key.equals("ABOVENORMAL")) {
            key = "ABOVE_NORMAL";
        }
        try {
            return Optional.of(Level.valueOf(key));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static void setPriorityClass(Level level) throws Throwable {
        Linker       linker   = Linker.nativeLinker();
        SymbolLookup kernel32 = SymbolLookup.libraryLookup("kernel32", Arena.global());

        MethodHandle getCurrentProcess = linker.downcallHandle(
                kernel32.find("GetCurrentProcess")
                        .orElseThrow(),
                FunctionDescriptor.of(ValueLayout.ADDRESS));

        Linker.Option captureError = Linker.Option.captureCallState("GetLastError");
        StructLayout  stateLayout  = Linker.Option.captureStateLayout();
        VarHandle     lastError    = stateLayout.varHandle(MemoryLayout.PathElement.groupElement("GetLastError"));
        MethodHandle setPriorityClass = linker.downcallHandle(
                kernel32.find("SetPriorityClass")
                        .orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT),
                captureError);
        MethodHandle getPriorityClass = linker.downcallHandle(
                kernel32.find("GetPriorityClass")
                        .orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

        // A pseudo handle for this process: needs no closing.
        MemorySegment process = (MemorySegment) getCurrentProcess.invokeExact();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment state = arena.allocate(stateLayout);
            int           ok    = (int) setPriorityClass.invokeExact(state, process, level.windowsClass);
            if (ok == 0) {
                int error = (int) lastError.get(state, 0L);
                log.warn("Could not set the process priority to {}: Windows error {}; running at normal priority",
                        level.displayName, error);
                return;
            }
        }
        int now = (int) getPriorityClass.invokeExact(process);
        log.info("Process priority set to {} (priority class 0x{})", level.displayName, Integer.toHexString(now));
    }
}
