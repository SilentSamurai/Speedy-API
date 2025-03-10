
package com.github.silent.samurai.speedy.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.helpers.MetadataUtil;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializer;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.request.IResponseContext;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.request.UpdateDataHandler;
import com.github.silent.samurai.speedy.serializers.JSONSerializer;
import com.github.silent.samurai.speedy.serializers.JSONSerializerV2;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class UpdateHandler implements Handler {

    final Handler next;

    public UpdateHandler(Handler handler) {
        this.next = handler;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        EntityMetadata entityMetadata = context.getEntityMetadata();

        Optional<SpeedyEntity> savedEntity;
        try {
            UpdateParams up = parse(context);
            savedEntity = runBatch(context, up);
        } catch (Exception e) {
            throw new InternalServerError("Update failed", e);
        }

        if (savedEntity.isEmpty()) {
            throw new NotFoundException();
        }

        List<SpeedyEntity> speedyEntities = List.of(savedEntity.get());
        context.setResponseSerializer(new JSONSerializerV2(
                speedyEntities,
                0,
                new ArrayList<>()
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

    private Optional<SpeedyEntity> runBatch(RequestContext context, UpdateParams updateParams) throws Exception {
        EntityMetadata entityMetadata = context.getEntityMetadata();
        EventProcessor eventProcessor = context.getEventProcessor();
        SpeedyEntity entity = updateParams.entity;
        SpeedyEntityKey pk = updateParams.pk;

        SpeedyEntity savedEntity = null;
        if (entity != null) {
            // trigger b4 validate
            eventProcessor.triggerEvent(SpeedyEventType.PRE_UPDATE, entityMetadata, entity);

            context.getValidationProcessor().validateUpdateRequestEntity(entityMetadata, entity);

            QueryProcessor queryProcessor = context.getQueryProcessor();
            savedEntity = queryProcessor.update(pk, entity);

            eventProcessor.triggerEvent(SpeedyEventType.POST_UPDATE, entityMetadata, entity);
        }

        return Optional.ofNullable(savedEntity);
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
