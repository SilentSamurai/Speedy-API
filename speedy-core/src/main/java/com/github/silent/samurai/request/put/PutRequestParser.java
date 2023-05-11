package com.github.silent.samurai.request.put;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.helpers.MetadataUtil;
import com.github.silent.samurai.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
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
        parser.parse();
        context.setParser(parser);

        ObjectMapper json = CommonUtil.json();
        JsonNode jsonElement = json.readTree(context.getRequest().getReader());
        if (jsonElement == null || !jsonElement.isObject()) {
            throw new BadRequestException("no content to process");
        }
        if (!parser.getPrimaryResource().isOnlyIdentifiersPresent()) {
            throw new BadRequestException("Primary Key Incomplete.");
        }
        EntityMetadata entityMetadata = parser.getPrimaryResource().getResourceMetadata();
        Object pk = MetadataUtil.createIdentifierFromParser(parser);
        Object entityInstance = context.getEntityManager().find(entityMetadata.getEntityClass(), pk);
        MetadataUtil.updateEntityFromJSON(entityMetadata, context.getEntityManager(), (ObjectNode) jsonElement, entityInstance);
        context.setEntityInstance(entityInstance);
        LOGGER.info(" test {}", entityInstance);
    }

}
