package com.github.curiousoddman.curious_tunes.actions.services;

import com.github.curiousoddman.curious_tunes.actions.dao.PendingActionDao;
import com.github.curiousoddman.curious_tunes.actions.handler.PendingActionsHandler;
import com.github.curiousoddman.curious_tunes.dbobj.tables.records.PendingActionRecord;
import com.github.curiousoddman.curious_tunes.util.StartupRunnable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PendingActionService implements StartupRunnable {
    private final PendingActionDao dao;
    private final Map<String, PendingActionsHandler<?>> handlers;

    public PendingActionService(PendingActionDao dao, List<PendingActionsHandler<?>> handlerList) {
        this.dao = dao;
        this.handlers = new HashMap<>();
        handlerList.forEach(h -> {
            log.info("Registering handler for type: {} from {}", h.getHandledType(), h.getClass());
            handlers.put(h.getHandledType(), h);
        });
    }

    @Override
    @Transactional
    public void onStartup() {
        log.info("Executing pending actions...");
        dao.resetStuckActions();
        List<PendingActionRecord> actions = dao.findExecutable();
        log.info("Found {} pending actions to execute.", actions.size());
        for (PendingActionRecord action : actions) {
            process(action);

        }
        log.info("Pending actions execution completed.");
    }

/*    public void updateLyrics(String newLyrics, Path filePath) {
        UpdateLyricsPayload payload = new UpdateLyricsPayload(newLyrics, filePath);
        PendingActionRecord action = dao.newAction(payload);
        process(action);
    }*/

    private void process(PendingActionRecord action) {
        PendingActionsHandler<?> handler = handlers.get(action.getType());
        if (handler == null) {
            dao.markFailed(action.getId(), "No handler for type");
            return;
        }

        try {
            dao.markInProgress(action.getId());
            handler.execute(action);
            dao.markCompleted(action.getId());
        } catch (Exception e) {
            dao.retryLater(action, e);
        }
    }
}