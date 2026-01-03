package com.github.curiousoddman.curious_tunes;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableCaching
@SpringBootApplication
@EnableTransactionManagement
public class Main {
    public static void main(String[] args) {
        Application.launch(JavafxApplication.class, args);
    }
}
