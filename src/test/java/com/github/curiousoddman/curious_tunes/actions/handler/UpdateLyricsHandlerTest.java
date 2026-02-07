package com.github.curiousoddman.curious_tunes.actions.handler;

import com.github.curiousoddman.curious_tunes.actions.payload.UpdateLyricsPayload;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.PendingActionRecord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UpdateLyricsHandlerTest {

    @Test
    void testHandledType() {
        UpdateLyricsHandler handler = new UpdateLyricsHandler(null);
        assertEquals(UpdateLyricsPayload.class.getName(), handler.getHandledType());
    }

    @Test
    void testDeserialize() throws Exception {
        UpdateLyricsHandler handler = new UpdateLyricsHandler(null);
        UpdateLyricsPayload payload = new UpdateLyricsPayload("New lyrics", null);
        String json = PendingActionsHandler.OBJECT_MAPPER.writeValueAsString(payload);

        // Simulate a PendingActionRecord with the JSON payload
        PendingActionRecord record = new com.github.curiousoddman.curious_tunes.dbobj.tables.records.PendingActionRecord();
        record.setPayload(json.getBytes());

        UpdateLyricsPayload deserialized = handler.deserialize(record);
        assertEquals("New lyrics", deserialized.getLyrics());
        assertNull(deserialized.getPath());
    }
}