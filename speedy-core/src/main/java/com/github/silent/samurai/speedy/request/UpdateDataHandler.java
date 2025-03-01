package com.github.silent.samurai.speedy.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.helpers.MetadataUtil;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

public class UpdateDataHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDataHandler.class);

    private final IRequestContextImpl context;
    private SpeedyEntity entity;
    private SpeedyEntityKey pk;

    public UpdateDataHandler(IRequestContextImpl context) {
        this.context = context;
    }

    public Optional<SpeedyEntity> process() throws Exception {
        EntityMetadata entityMetadata = context.getEntityMetadata();
        if (!entityMetadata.isUpdateAllowed()) {
            throw new BadRequestException(String.format("update not allowed for %s", entityMetadata.getName()));
        }
        parse();
        return runBatch();
    }

    private void parse() throws SpeedyHttpException, IOException {

        ObjectMapper json = CommonUtil.json();
        JsonNode jsonElement = json.readTree(context.getRequest().getReader());

        if (jsonElement == null || !jsonElement.isObject()) {
            throw new BadRequestException("no content to process");
        }

        EntityMetadata entityMetadata = context.getEntityMetadata();

        pk = MetadataUtil.createIdentifierFromJSON(entityMetadata, (ObjectNode) jsonElement);
        if (!context.getQueryProcessor().exists(pk)) {
            throw new BadRequestException("Entity not present.");
        }

        entity = MetadataUtil.createEntityFromJSON(entityMetadata, (ObjectNode) jsonElement);

        LOGGER.info(" pk {} -> entity {}", pk, entity);
    }

    private Optional<SpeedyEntity> runBatch() throws Exception {
        EntityMetadata entityMetadata = context.getEntityMetadata();
        EventProcessor eventProcessor = context.getEventProcessor();
        SpeedyEntity savedEntity = null;
        if (this.entity != null) {
            // trigger b4 validate
            eventProcessor.triggerEvent(SpeedyEventType.PRE_UPDATE, entityMetadata, entity);

            context.getValidationProcessor().validateUpdateRequestEntity(entityMetadata, entity);

            QueryProcessor queryProcessor = context.getQueryProcessor();
            savedEntity = queryProcessor.update(this.pk, entity);

            eventProcessor.triggerEvent(SpeedyEventType.POST_UPDATE, entityMetadata, entity);
        }

        return Optional.ofNullable(savedEntity);
    }
}
