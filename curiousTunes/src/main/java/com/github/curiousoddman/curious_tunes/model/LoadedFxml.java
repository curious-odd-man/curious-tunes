package com.github.curiousoddman.curious_tunes.model;

import javafx.scene.Parent;

public record LoadedFxml<T>(Parent parent, T controller) {
}
