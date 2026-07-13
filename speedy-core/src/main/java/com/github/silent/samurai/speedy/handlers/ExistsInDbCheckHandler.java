package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.PreconditionFailedException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.models.SpeedyDeleteBody;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.models.SpeedyHeaders;
import com.github.silent.samurai.speedy.models.SpeedyUpdateBody;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Loads and verifies all PATCH/PUT/DELETE targets before downstream authorization or write
/// handlers run.
///
/// The handler stores the persisted rows in {@link DbCheckEntities}; it performs no update
/// events or writes. A missing target stops the whole request before the write handler is
/// reached. `If-Match` never applies to a bulk (multi-item) request — it is rejected up front,
/// before existence or authorization is even checked.
public class ExistsInDbCheckHandler implements Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        SpeedyBody body = context.get(SpeedyBody.class);
        List<SpeedyEntityKey> keys = keysIn(body);
        if (keys.isEmpty()) {
            context.put(new DbCheckEntities(Map.of()));
            return;
        }

        SpeedyHeaders headers = context.get(SpeedyHeaders.class);
        String ifMatch = headers.get("If-Match");
        if (ifMatch != null && !ifMatch.isBlank() && keys.size() > 1) {
            throw new BadRequestException("If-Match is only supported for single-entity requests");
        }

        QueryProcessor queryProcessor = context.get(QueryProcessor.class);
        Map<SpeedyEntityKey, SpeedyEntity> existingRows = new LinkedHashMap<>();

        for (SpeedyEntityKey key : keys) {
            SpeedyEntity existingRow = queryProcessor.fetchByKey(key).orElse(null);
            if (existingRow == null) {
                rejectMissingTarget(context, ifMatch, key);
            } else {
                existingRows.put(key, existingRow);
            }
        }

        context.put(new DbCheckEntities(existingRows));
    }

    private List<SpeedyEntityKey> keysIn(SpeedyBody body) {
        if (body instanceof SpeedyUpdateBody updateBody) {
            return updateBody.getItems().stream().map(SpeedyUpdateBody.Item::getPk).toList();
        }
        if (body instanceof SpeedyDeleteBody deleteBody) {
            return deleteBody.getKeys();
        }
        return List.of();
    }

    private void rejectMissingTarget(SpeedyContext context, String ifMatch, SpeedyEntityKey key)
            throws SpeedyHttpException {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new NotFoundException("entity not found: " + key);
        }

        // A bulk If-Match request was already rejected above, so a single target is guaranteed here.
        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();
        if (entityMetadata.getVersionField().isEmpty()) {
            throw new BadRequestException(
                    "If-Match is not supported for entity '" + entityMetadata.getName() + "' (no @SpeedyETag field)");
        }
        throw new PreconditionFailedException(
                "If-Match precondition failed: " + entityMetadata.getName() + " not found");
    }
}
