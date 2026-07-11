package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponse;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityResponse;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.utils.EtagUtil;

import java.util.List;

/// Runs after create/update/replace: for an entity with a version field and a response carrying
/// exactly one saved row, adds the fresh `ETag` header — computed from the row {@link
/// EtagStampHandler} already stamped and the operation persisted — so a client can chain it into
/// a later `If-Match`. Zero works otherwise (no version field, or a bulk/batch/error response).
public class WriteEtagHandler implements Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();
        if (entityMetadata.getVersionField().isEmpty()) {
            return;
        }

        SpeedyResponse response = context.get(SpeedyResponse.class);
        if (!(response instanceof SpeedyEntityResponse entityResponse)) {
            return;
        }
        List<? extends SpeedyValue> payload = entityResponse.getPayload();
        if (payload.size() != 1 || !(payload.get(0) instanceof SpeedyEntity savedEntity)) {
            return;
        }

        EtagUtil.computeEtag(savedEntity).ifPresent(etag -> entityResponse.putHeader("ETag", etag));
    }
}
