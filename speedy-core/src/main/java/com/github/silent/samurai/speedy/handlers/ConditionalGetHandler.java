package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponse;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityResponse;
import com.github.silent.samurai.speedy.models.SpeedyHeaders;
import com.github.silent.samurai.speedy.models.SpeedyNotModifiedResponse;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.utils.EtagUtil;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/// Runs after {@link GetHandler}: for a genuinely single-resource result (the query resolved to
/// exactly one row, not a paginated slice of a larger match) on an entity that declares a version
/// field, emits an `ETag` response header and, if the client's `If-None-Match` matches, replaces
/// the response with a bodiless 304. Zero work for every other entity or result shape — a
/// multi-row list GET never gets an ETag in v1.
///
/// @see GetHandler
/// @see EtagUtil
public class ConditionalGetHandler implements Handler {

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
        // totalCount==1, not just payload.size()==1: a paginated slice of a larger match (e.g.
        // $top=1) is not a stable single resource, even though exactly one row comes back.
        if (!BigInteger.ONE.equals(entityResponse.getTotalCount())) {
            return;
        }
        List<? extends SpeedyValue> payload = entityResponse.getPayload();
        if (payload.size() != 1 || !(payload.get(0) instanceof SpeedyEntity singleEntity)) {
            return;
        }

        Optional<String> etag = EtagUtil.computeEtag(singleEntity);
        if (etag.isEmpty()) {
            return;
        }

        SpeedyHeaders headers = context.get(SpeedyHeaders.class);
        String ifNoneMatch = headers.get("If-None-Match");
        if (ifNoneMatch != null && !ifNoneMatch.isBlank()
                && EtagUtil.ifNoneMatchSatisfied(ifNoneMatch, etag.get())) {
            context.put(SpeedyResponse.class, SpeedyNotModifiedResponse.builder()
                    .headers(Map.of("ETag", etag.get()))
                    .build());
            return;
        }

        entityResponse.putHeader("ETag", etag.get());
    }
}
