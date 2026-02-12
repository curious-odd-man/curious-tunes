package com.github.curiousoddman.curious_tunes.actions.handler;
/*
import com.github.curiousoddman.curious_tunes.actions.payload.UpdateLyricsPayload;
import com.github.curiousoddman.curious_tunes.backend.tags.MetadataManager;
import com.github.curiousoddman.curious_tunes.backend.tags.MetadataTags;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.PendingActionRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateLyricsHandler extends PendingActionsHandler<UpdateLyricsPayload> {
    private final MetadataManager metadataManager;

    @Override
    public void execute(PendingActionRecord action) throws Exception {
        UpdateLyricsPayload payload = deserialize(action);
        MetadataTags metadata = metadataManager.getMetadata(payload.getPath());
        metadata.setLyrics(payload.getLyrics());
        metadata.updateFile();
    }
}*/
