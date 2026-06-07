package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.enums.TransactionMode;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpRuntimeException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.models.SpeedyEntityResponse;
import com.github.silent.samurai.speedy.models.SpeedyUpdateBody;
import com.github.silent.samurai.speedy.request.RequestContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/// Handles PUT/PATCH /{Entity}/$update requests with pre-parsed SpeedyUpdateBody.
///
/// Reads the SpeedyUpdateBody (parsed from JSON by JSONBodyParser and set as
/// body by BodyParserHandler), fires PRE/POST_UPDATE events, validates the entity,
/// and updates it in a single transaction.
///
/// @see BodyParserHandler
/// @see SpeedyUpdateBody
@Slf4j
public class UpdateHandler implements Handler {

    final Handler next;

    public UpdateHandler(Handler handler) {
        this.next = handler;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        SpeedyUpdateBody body = (SpeedyUpdateBody) context.getRequest().getBody();
        SpeedyEntity savedEntity = updateInTransaction(context, body.getEntity(), body.getPk());

        if (savedEntity == null) {
            throw new NotFoundException();
        }

        List<SpeedyEntity> speedyEntities = List.of(savedEntity);
        context.setSpeedyResponse(
                SpeedyEntityResponse.builder()
                        .payload(speedyEntities)
                        .pageIndex(0)
                        .status(200)
                        .build()
        );

        next.process(context);
    }

    private SpeedyEntity updateInTransaction(RequestContext context, SpeedyEntity entity, SpeedyEntityKey pk)
            throws SpeedyHttpException {
        EntityMetadata entityMetadata = context.getEntityMetadata();
        EventProcessor eventProcessor = context.getEventProcessor();
        QueryProcessor queryProcessor = context.getQueryProcessor();
        TransactionMode mode = context.getRequest().getTransactionMode();
        String entityLabel = entityMetadata.getName();

        try {
            SpeedyEntity[] result = new SpeedyEntity[1];
            queryProcessor.runInTransaction(() -> {
                try {
                    eventProcessor.triggerEvent(SpeedyEventType.PRE_UPDATE, entityMetadata, entity);
                    context.getValidationProcessor().validateUpdateRequestEntity(entityMetadata, entity);
                    result[0] = queryProcessor.update(pk, entity);
                    eventProcessor.triggerEvent(SpeedyEventType.POST_UPDATE, entityMetadata, entity);
                } catch (Exception ex) {
                    if (ex instanceof SpeedyHttpRuntimeException re) throw re;
                    if (ex instanceof RuntimeException re) throw re;
                    if (ex instanceof SpeedyHttpException she) {
                        throw new SpeedyHttpRuntimeException(she.getStatus(), she);
                    }
                    throw new SpeedyHttpRuntimeException(500, ex);
                }
            });

            log.info("Update committed: entity={}, mode={}, pk={}", entityLabel, mode, pk);
            return result[0];
        } catch (Exception e) {
            log.info("Update rolled back: entity={}, mode={}, pk={}", entityLabel, mode, pk);
            if (e instanceof SpeedyHttpException she) {
                throw she;
            }
            if (e instanceof SpeedyHttpRuntimeException sre) {
                throw new SpeedyHttpException(sre.getStatus(), sre.getMessage(), sre);
            }
            if (e.getCause() instanceof SpeedyHttpException she) {
                throw she;
            }
            if (e.getCause() instanceof SpeedyHttpRuntimeException sre) {
                throw new SpeedyHttpException(sre.getStatus(), sre.getMessage(), sre);
            }
            throw new InternalServerError("Update failed", e);
        }
    }
}
