package com.github.curiousoddman.curious_tunes.ui.controller.screen;

import com.github.curiousoddman.curious_tunes.domain.user.prefs.UserPreferencesService;
import com.github.curiousoddman.curious_tunes.event.RescanLibraryEvent;
import com.github.curiousoddman.curious_tunes.model.bundle.RescanBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ResourceBundle;

@Lazy
@Component
@RequiredArgsConstructor
public class RescanLibraryController implements Initializable {
    private final ApplicationEventPublisher eventPublisher;
    private final UserPreferencesService    userPreferencesService;
    @FXML
    public        ComboBox<String>          path;
    @FXML
    public        Label                     errorLabel;
    @FXML
    public        Button                    scanButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        path.getItems()
            .setAll(userPreferencesService.getRecentLibraryRoots());
        if (resources instanceof RescanBundle bundle) {
            path.getEditor()
                .setText(bundle.getLibraryRoot());
        }
        path.getEditor()
            .textProperty()
            .addListener((obs, oldValue, newValue) -> validatePath());
        validatePath();
        Platform.runLater(() -> path.getEditor()
                                    .requestFocus());
    }

    @FXML
    public void onChoosePath(ActionEvent actionEvent) {
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("Select library root directory");
        if (isDirectory(enteredPath())) {
            fileChooser.setInitialDirectory(new File(enteredPath()));
        }
        File result = fileChooser.showDialog(((Node) actionEvent.getSource()).getScene()
                                                                             .getWindow());
        if (result == null) {
            return;     // the dialog was cancelled
        }
        path.getEditor()
            .setText(result.getAbsolutePath());
    }

    @FXML
    public void onRescan(ActionEvent actionEvent) {
        String libraryRoot = enteredPath();
        if (!isDirectory(libraryRoot)) {
            return;
        }
        userPreferencesService.saveLibraryRoot(libraryRoot);
        eventPublisher.publishEvent(new RescanLibraryEvent(this, libraryRoot));
        close();
    }

    @FXML
    public void onCancel(ActionEvent actionEvent) {
        close();
    }

    private void close() {
        ((Stage) path.getScene()
                     .getWindow()).close();
    }

    /**
     * Scan is possible only for an existing folder; otherwise the button is disabled and the reason is shown.
     */
    private void validatePath() {
        String text  = enteredPath();
        String error = null;
        if (!text.isEmpty() && !isDirectory(text)) {
            error = "This folder does not exist.";
        }
        errorLabel.setText(error == null ? "" : error);
        scanButton.setDisable(text.isEmpty() || error != null);
    }

    private String enteredPath() {
        String text = path.getEditor()
                          .getText();
        return text == null ? "" : text.trim();
    }

    private static boolean isDirectory(String text) {
        if (text.isEmpty()) {
            return false;
        }
        try {
            return Files.isDirectory(Path.of(text));
        } catch (InvalidPathException e) {
            return false;
        }
    }
}
