package com.github.silent.samurai.speedy.request.delete;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.helpers.MetadataUtil;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteRequestParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteRequestParser.class);

    private final DeleteRequestContext context;

    public DeleteRequestParser(DeleteRequestContext context) {
        this.context = context;
    }

    public void process() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(context.getMetaModelProcessor(), context.getRequestURI());
        parser.parse();
        context.setParser(parser);
        EntityMetadata resourceMetadata = parser.getPrimaryResource().getResourceMetadata();

        ObjectMapper json = CommonUtil.json();
        JsonNode jsonElement = json.readTree(context.getRequest().getReader());
        if (jsonElement == null || !jsonElement.isArray()) {
            throw new BadRequestException("in-valid request");
        }
        ArrayNode batchOfEntities = (ArrayNode) jsonElement;
        for (JsonNode element : batchOfEntities) {
            if (element.isObject()) {
                if (!MetadataUtil.isPrimaryKeyComplete(resourceMetadata, Sets.newHashSet(element.fieldNames()))) {
                    throw new BadRequestException("Primary Key Incomplete ");
                }
                Object pk = MetadataUtil.createIdentifierFromJSON(resourceMetadata, (ObjectNode) element);
                Object entityInstance = context.getEntityManager().find(resourceMetadata.getEntityClass(), pk);
                if (entityInstance == null) {
                    throw new BadRequestException("entity not found");
                }
                context.getObjectsToBeRemoved().add(entityInstance);
                LOGGER.info("parsed primary key {}", pk);
            } else {
                throw new BadRequestException("in-valid request body");
            }
        }
    }

}
