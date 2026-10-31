package com.github.curiousoddman.curious_tunes.ui.controller.screen;

import com.github.curiousoddman.curious_tunes.domain.user.prefs.UserPreferencesService;
import com.github.curiousoddman.curious_tunes.event.UserNotificationEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.text.Font;
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

/**
 * The few settings of the application: music folder, what the play buttons do, size of the lyrics text.
 */
@Lazy
@Component
@RequiredArgsConstructor
public class SettingsController implements Initializable {
    private final UserPreferencesService    userPreferencesService;
    private final ApplicationEventPublisher eventPublisher;

    @FXML
    public TextField   folderField;
    @FXML
    public RadioButton inOrderRadio;
    @FXML
    public RadioButton shuffledRadio;
    @FXML
    public Slider      lyricsSizeSlider;
    @FXML
    public Label       lyricsSizeLabel;
    @FXML
    public Label       lyricsPreview;
    @FXML
    public Label       errorLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        folderField.setText(userPreferencesService.getLibraryRoot());
        (userPreferencesService.isPlayShuffled() ? shuffledRadio : inOrderRadio).setSelected(true);
        lyricsSizeSlider.valueProperty()
                        .addListener((obs, oldValue, newValue) -> showLyricsSize(newValue.doubleValue()));
        lyricsSizeSlider.setValue(userPreferencesService.getLyricsFontSize());
        showLyricsSize(lyricsSizeSlider.getValue());
        errorLabel.setText("");
    }

    private void showLyricsSize(double size) {
        lyricsSizeLabel.setText(Math.round(size) + " pt");
        lyricsPreview.setFont(Font.font(Math.round(size)));
    }

    @FXML
    public void onChooseFolder(ActionEvent actionEvent) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select the music folder");
        File current = new File(folderField.getText() == null ? "" : folderField.getText()
                                                                                .trim());
        if (current.isDirectory()) {
            chooser.setInitialDirectory(current);
        }
        File result = chooser.showDialog(folderField.getScene()
                                                    .getWindow());
        if (result != null) {
            folderField.setText(result.getAbsolutePath());
        }
    }

    @FXML
    public void onSave(ActionEvent actionEvent) {
        String folder = folderField.getText() == null ? "" : folderField.getText()
                                                                        .trim();
        if (!folder.isEmpty() && !isDirectory(folder)) {
            errorLabel.setText("The music folder does not exist.");
            return;
        }
        if (!folder.isEmpty()) {
            userPreferencesService.saveLibraryRoot(folder);
        }
        userPreferencesService.savePlayShuffled(shuffledRadio.isSelected());
        userPreferencesService.saveLyricsFontSize(Math.round(lyricsSizeSlider.getValue()));
        eventPublisher.publishEvent(UserNotificationEvent.info(this, "Settings saved"));
        close();
    }

    @FXML
    public void onCancel(ActionEvent actionEvent) {
        close();
    }

    private void close() {
        ((Stage) folderField.getScene()
                            .getWindow()).close();
    }

    private static boolean isDirectory(String text) {
        try {
            return Files.isDirectory(Path.of(text));
        } catch (InvalidPathException e) {
            return false;
        }
    }
}
