package com.github.curiousoddman.curious_tunes;

import com.github.curiousoddman.curious_tunes.app.preloader.AnimatedPreloader;
import com.github.curiousoddman.curious_tunes.util.ProcessPriority;
import com.sun.javafx.application.LauncherImpl;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class Main {
    public static void main(String[] args) {
        ProcessPriority.apply();        // first, so every thread started later gets the higher priority
        LauncherImpl.launchApplication(JavafxApplication.class, AnimatedPreloader.class, args);
    }
}
