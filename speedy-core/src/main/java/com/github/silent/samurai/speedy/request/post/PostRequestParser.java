package com.github.silent.samurai.speedy.request.post;


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

public class PostRequestParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostRequestParser.class);

    private final PostRequestContext context;

    public PostRequestParser(PostRequestContext context) {
        this.context = context;
    }

    public void processBatch() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(context.getMetaModelProcessor(), context.getRequestURI());
        parser.parse();
        context.setParser(parser);

        ObjectMapper json = CommonUtil.json();
        JsonNode jsonElement = json.readTree(context.getRequest().getReader());
        if (jsonElement == null || !jsonElement.isArray()) {
            throw new BadRequestException("no content to process");
        }
        ArrayNode batchOfEntities = (ArrayNode) jsonElement;
        EntityMetadata resourceMetadata = parser.getPrimaryResource().getResourceMetadata();
        for (JsonNode element : batchOfEntities) {
            if (element.isObject()) {
                ObjectNode objectNode = (ObjectNode) element;

                if (MetadataUtil.isPrimaryKeyComplete(resourceMetadata, Sets.newHashSet(objectNode.fieldNames()))) {
                    Object pk = MetadataUtil.createIdentifierFromJSON(
                            resourceMetadata,
                            objectNode);
                    if (pk != null) {
                        Object entityInDb = context.getEntityManager().find(resourceMetadata.getEntityClass(), pk);
                        if (entityInDb != null) {
                            throw new BadRequestException("Entity already present.");
                        }
                    }
                }

                Object entityInstance = MetadataUtil.createEntityFromJSON(
                        resourceMetadata,
                        objectNode,
                        context.getEntityManager()
                );
                LOGGER.info("parsed entity {}", entityInstance);
                context.getParsedObjects().add(entityInstance);
            } else {
                throw new BadRequestException("in-valid content");
            }
        }
    }

}
