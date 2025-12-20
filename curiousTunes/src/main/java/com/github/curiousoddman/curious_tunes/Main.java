package com.github.curiousoddman.curious_tunes;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class Main {
    public static void main(String[] args) {
        Application.launch(JavafxApplication.class, args);
    }
}
