package com.github.curiousoddman.curious_tunes.model;

import com.github.curiousoddman.curious_tunes.model.info.TrackInfo;
import javafx.scene.control.SingleSelectionModel;

public class TrackSelectionModel extends SingleSelectionModel<TrackInfo> {
    @Override
    protected TrackInfo getModelItem(int index) {
        return null;
    }

    @Override
    protected int getItemCount() {
        return 0;
    }
}
