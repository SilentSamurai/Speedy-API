package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.PreconditionFailedException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.models.*;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.utils.EtagUtil;

import java.util.List;
import java.util.Optional;

/// Honors `If-Match` on PATCH/PUT/DELETE. A request with no `If-Match` is untouched (zero cost).
/// Otherwise: a multi-item batch carrying the header is rejected with 400 (a single header value
/// can't address N resources); an entity with no version field is rejected with 400 (no ETag
/// protection is possible, so the client is never falsely assured of one); and otherwise the
/// row's current state is fetched and compared — a missing row or a tag mismatch is 412.
///
/// @see EtagUtil
public class PreconditionCheckHandler implements Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        SpeedyHeaders headers = context.get(SpeedyHeaders.class);
        String ifMatch = headers.get("If-Match");
        if (ifMatch == null || ifMatch.isBlank()) {
            return;
        }

        List<SpeedyEntityKey> pks = pksIn(context.get(SpeedyBody.class));
        if (pks.isEmpty()) {
            return;
        }
        if (pks.size() > 1) {
            throw new BadRequestException("If-Match is only supported for single-entity requests");
        }

        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();
        if (entityMetadata.getVersionField().isEmpty()) {
            throw new BadRequestException(
                    "If-Match is not supported for entity '" + entityMetadata.getName() + "' (no @SpeedyETag field)");
        }

        QueryProcessor queryProcessor = context.get(QueryProcessor.class);
        Optional<SpeedyEntity> current = queryProcessor.fetchByKey(pks.get(0));
        if (current.isEmpty()) {
            // If-Match (any value, including "*") against a resource that doesn't exist always fails.
            throw new PreconditionFailedException(
                    "If-Match precondition failed: " + entityMetadata.getName() + " not found");
        }

        String currentEtag = EtagUtil.computeEtag(current.get()).orElse(null);
        if (!EtagUtil.ifMatchSatisfied(ifMatch, currentEtag)) {
            throw new PreconditionFailedException("If-Match precondition failed for " + entityMetadata.getName());
        }
    }

    private List<SpeedyEntityKey> pksIn(SpeedyBody body) {
        if (body instanceof SpeedyUpdateBody updateBody) {
            return updateBody.getItems().stream().map(SpeedyUpdateBody.Item::getPk).toList();
        }
        if (body instanceof SpeedyDeleteBody deleteBody) {
            return deleteBody.getKeys();
        }
        return List.of();
    }
}
