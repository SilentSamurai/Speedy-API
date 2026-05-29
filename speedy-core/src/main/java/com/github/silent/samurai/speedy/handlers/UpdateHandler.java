package com.github.silent.samurai.speedy.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.enums.TransactionMode;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.helpers.MetadataUtil;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.serializers.JSONSerializerV2;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;

@Slf4j
public class UpdateHandler implements Handler {

    final Handler next;

    public UpdateHandler(Handler handler) {
        this.next = handler;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        EntityMetadata entityMetadata = context.getEntityMetadata();

        UpdateParams up = parse(context);
        SpeedyEntity savedEntity = updateInTransaction(context, up);

        if (savedEntity == null) {
            throw new NotFoundException();
        }

        List<SpeedyEntity> speedyEntities = List.of(savedEntity);
        context.setResponseSerializer(new JSONSerializerV2(
                speedyEntities,
                0,
                new HashSet<>()
        ));

        next.process(context);
    }

    private UpdateParams parse(RequestContext context) throws SpeedyHttpException {

        ObjectMapper json = CommonUtil.json();
        JsonNode jsonBody = context.getBody();

        if (jsonBody == null || !jsonBody.isObject()) {
            throw new BadRequestException("no content to process");
        }

        EntityMetadata entityMetadata = context.getEntityMetadata();

        SpeedyEntityKey pk = MetadataUtil.createIdentifierFromJSON(entityMetadata, (ObjectNode) jsonBody);
        if (!context.getQueryProcessor().exists(pk)) {
            throw new BadRequestException("Entity not present.");
        }

        SpeedyEntity entity = MetadataUtil.createEntityFromJSON(entityMetadata, (ObjectNode) jsonBody);

        log.info(" pk {} -> entity {}", pk, entity);
        return new UpdateParams(pk, entity);
    }

    private SpeedyEntity updateInTransaction(RequestContext context, UpdateParams updateParams)
            throws SpeedyHttpException {
        EntityMetadata entityMetadata = context.getEntityMetadata();
        EventProcessor eventProcessor = context.getEventProcessor();
        QueryProcessor queryProcessor = context.getQueryProcessor();
        TransactionMode mode = context.getTransactionMode();
        String entityLabel = entityMetadata.getName();
        SpeedyEntity entity = updateParams.entity;
        SpeedyEntityKey pk = updateParams.pk;

        try {
            SpeedyEntity[] result = new SpeedyEntity[1];
            queryProcessor.runInTransaction(() -> {
                try {
                    eventProcessor.triggerEvent(SpeedyEventType.PRE_UPDATE, entityMetadata, entity);
                    context.getValidationProcessor().validateUpdateRequestEntity(entityMetadata, entity);
                    result[0] = queryProcessor.update(pk, entity);
                    eventProcessor.triggerEvent(SpeedyEventType.POST_UPDATE, entityMetadata, entity);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });

            log.info("Update committed: entity={}, mode={}, pk={}",
                entityLabel, mode, pk);
            return result[0];
        } catch (Exception e) {
            log.info("Update rolled back: entity={}, mode={}, pk={}",
                entityLabel, mode, pk);
            if (e instanceof SpeedyHttpException) {
                throw (SpeedyHttpException) e;
            }
            if (e.getCause() instanceof SpeedyHttpException) {
                throw (SpeedyHttpException) e.getCause();
            }
            throw new InternalServerError("Update failed", e);
        }
    }

    static class UpdateParams {
        SpeedyEntityKey pk;
        SpeedyEntity entity;

        public UpdateParams(SpeedyEntityKey pk, SpeedyEntity entity) {
            this.pk = pk;
            this.entity = entity;
        }

        boolean isPresent() {
            return pk != null && entity != null;
        }

    }
}
