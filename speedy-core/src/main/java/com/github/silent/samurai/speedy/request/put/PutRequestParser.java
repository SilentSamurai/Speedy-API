package com.github.silent.samurai.speedy.request.put;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.helpers.MetadataUtil;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PutRequestParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(PutRequestParser.class);

    private final PutRequestContext context;

    public PutRequestParser(PutRequestContext context) {
        this.context = context;
    }

    public void process() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(context.getMetaModelProcessor(), context.getRequestURI());
        SpeedyQuery speedyQuery = parser.parse();
        context.setSpeedyQuery(speedyQuery);

        ObjectMapper json = CommonUtil.json();
        JsonNode jsonElement = json.readTree(context.getRequest().getReader());
        if (jsonElement == null || !jsonElement.isObject()) {
            throw new BadRequestException("no content to process");
        }
        if (!speedyQuery.isOnlyIdentifiersPresent()) {
            throw new BadRequestException("Primary Key Incomplete.");
        }
        EntityMetadata entityMetadata = speedyQuery.getFrom();
        Object pk = MetadataUtil.createIdentifierFromParser(speedyQuery);
        Object entityInstance = context.getEntityManager().find(entityMetadata.getEntityClass(), pk);
        MetadataUtil.updateEntityFromJSON(entityMetadata, context.getEntityManager(), (ObjectNode) jsonElement, entityInstance);
        context.setEntityInstance(entityInstance);
        LOGGER.info(" test {}", entityInstance);
    }

}
