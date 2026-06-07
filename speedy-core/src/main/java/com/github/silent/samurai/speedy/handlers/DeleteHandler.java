package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.enums.TransactionMode;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpRuntimeException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.models.SpeedyBatchResponse;
import com.github.silent.samurai.speedy.models.SpeedyDeleteBody;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.models.SpeedyEntityResponse;
import com.github.silent.samurai.speedy.models.SpeedyPartialFailure;
import com.github.silent.samurai.speedy.request.RequestContext;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// Handles DELETE /{Entity}/$delete requests with pre-parsed SpeedyDeleteBody.
///
/// Reads the SpeedyDeleteBody (parsed from JSON by JSONBodyParser and set as
/// body by BodyParserHandler), fires PRE/POST_DELETE events, validates keys,
/// and bulk-deletes entities in either BATCH or PER_ENTITY transaction mode.
///
/// @see BodyParserHandler
/// @see SpeedyDeleteBody
@Slf4j
public class DeleteHandler implements Handler {

    final Handler next;

    public DeleteHandler(Handler handler) {
        this.next = handler;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        SpeedyDeleteBody body = (SpeedyDeleteBody) context.getRequest().getBody();
        List<SpeedyEntityKey> keys = body.getKeys();
        TransactionMode mode = body.getMode();

        if (mode == TransactionMode.BATCH) {
            processBatchDelete(context, keys);
        } else {
            processPerEntityDelete(context, keys);
        }

        next.process(context);
    }

    private void processBatchDelete(RequestContext context, List<SpeedyEntityKey> keys)
            throws SpeedyHttpException {
        EntityMetadata entityMetadata = context.getEntityMetadata();
        EventProcessor eventProcessor = context.getEventProcessor();
        QueryProcessor queryProcessor = context.getQueryProcessor();
        String entityLabel = entityMetadata.getName();
        int totalCount = keys.size();

        if (keys.isEmpty()) {
            context.setSpeedyResponse(
                    SpeedyEntityResponse.builder()
                            .payload(List.of())
                            .pageIndex(0)
                            .fieldPredicate(KeyFieldMetadata.class::isInstance)
                            .status(200)
                            .build()
            );
            return;
        }

        try {
            queryProcessor.runInTransaction(() -> {
                try {
                    for (SpeedyEntityKey key : keys) {
                        if (!queryProcessor.exists(key)) {
                            throw new BadRequestException("entity not found");
                        }
                        context.getValidationProcessor().validateDeleteRequestEntity(entityMetadata, key);
                        eventProcessor.triggerEvent(SpeedyEventType.PRE_DELETE, entityMetadata, key);
                    }

                    List<SpeedyEntity> deleted = queryProcessor.delete(keys);

                    for (SpeedyEntityKey key : keys) {
                        eventProcessor.triggerEvent(SpeedyEventType.POST_DELETE, entityMetadata, key);
                    }

                    context.setSpeedyResponse(
                            SpeedyEntityResponse.builder()
                                    .payload(deleted)
                                    .pageIndex(0)
                                    .fieldPredicate(KeyFieldMetadata.class::isInstance)
                                    .status(200)
                                    .build()
                    );
                } catch (Exception ex) {
                    if (ex instanceof SpeedyHttpRuntimeException re) throw re;
                    if (ex instanceof RuntimeException re) throw re;
                    if (ex instanceof SpeedyHttpException she) {
                        throw new SpeedyHttpRuntimeException(she.getStatus(), she);
                    }
                    throw new SpeedyHttpRuntimeException(500, ex);
                }
            });

            log.info("BATCH delete committed: entity={}, count={}", entityLabel, totalCount);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.info("BATCH delete rolled back: entity={}, count={}", entityLabel, totalCount);
            if (cause instanceof SpeedyHttpException she) {
                throw she;
            }
            if (cause instanceof SpeedyHttpRuntimeException sre) {
                throw new SpeedyHttpException(sre.getStatus(), sre.getMessage(), sre);
            }
            throw new InternalServerError("Batch delete failed", e);
        }
    }

    private void processPerEntityDelete(RequestContext context, List<SpeedyEntityKey> keys)
            throws SpeedyHttpException {
        EntityMetadata entityMetadata = context.getEntityMetadata();
        EventProcessor eventProcessor = context.getEventProcessor();
        QueryProcessor queryProcessor = context.getQueryProcessor();
        String entityLabel = entityMetadata.getName();

        List<SpeedyEntity> succeeded = new ArrayList<>();
        List<SpeedyPartialFailure> failed = new ArrayList<>();

        for (int i = 0; i < keys.size(); i++) {
            SpeedyEntityKey key = keys.get(i);
            try {
                queryProcessor.runInTransaction(() -> {
                    try {
                        if (!queryProcessor.exists(key)) {
                            throw new BadRequestException("entity not found");
                        }
                        context.getValidationProcessor().validateDeleteRequestEntity(entityMetadata, key);
                        eventProcessor.triggerEvent(SpeedyEventType.PRE_DELETE, entityMetadata, key);

                        List<SpeedyEntity> singleResult = queryProcessor.delete(List.of(key));

                        eventProcessor.triggerEvent(SpeedyEventType.POST_DELETE, entityMetadata, key);
                        if (!singleResult.isEmpty()) {
                            succeeded.add(singleResult.get(0));
                        }
                    } catch (Exception ex) {
                        if (ex instanceof SpeedyHttpRuntimeException re) throw re;
                        if (ex instanceof RuntimeException re) throw re;
                        if (ex instanceof SpeedyHttpException she) {
                            throw new SpeedyHttpRuntimeException(she.getStatus(), she);
                        }
                        throw new SpeedyHttpRuntimeException(500, ex);
                    }
                });
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                if (cause instanceof SpeedyHttpException she) {
                    failed.add(SpeedyPartialFailure.builder()
                            .index(i).status(she.getStatus())
                            .message(she.getMessage()).timestamp(Instant.now().toString())
                            .inputPk(key).cause(she).build());
                    log.info("Entity #{} failed in per-entity transaction", i, she);
                } else if (cause instanceof SpeedyHttpRuntimeException sre) {
                    failed.add(SpeedyPartialFailure.builder()
                            .index(i).status(sre.getStatus())
                            .message(sre.getMessage()).timestamp(Instant.now().toString())
                            .inputPk(key).cause(sre).build());
                    log.info("Entity #{} failed in per-entity transaction", i, sre);
                } else {
                    failed.add(SpeedyPartialFailure.builder()
                            .index(i).status(500)
                            .message(e.getMessage()).timestamp(Instant.now().toString())
                            .inputPk(key).cause(e).build());
                    log.info("Entity #{} failed in per-entity transaction", i, e);
                }
            }
        }

        log.info("Transaction committed: entity={}, mode=PER_ENTITY, count={}, succeeded={}, failed={}",
                entityLabel, keys.size(), succeeded.size(), failed.size());

        if (failed.isEmpty()) {
            context.setSpeedyResponse(
                    SpeedyEntityResponse.builder()
                            .payload(succeeded)
                            .pageIndex(0)
                            .fieldPredicate(KeyFieldMetadata.class::isInstance)
                            .status(200)
                            .build()
            );
        } else if (keys.size() == 1) {
            SpeedyPartialFailure failure = failed.get(0);
            if (failure.getStatus() == 400) {
                throw new BadRequestException(failure.getMessage(), failure.getCause());
            }
            throw new InternalServerError(failure.getMessage(), failure.getCause());
        } else {
            int status = succeeded.isEmpty() ? 400 : 207;
            context.setSpeedyResponse(
                    SpeedyBatchResponse.builder()
                            .succeeded(succeeded)
                            .failed(failed)
                            .pageIndex(0)
                            .status(status)
                            .build()
            );
        }
    }
}
