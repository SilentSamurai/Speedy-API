package com.github.silent.samurai.request.put;

import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.helpers.MetadataUtil;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.parser.SpeedyUriParser;
import com.github.silent.samurai.utils.CommonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PutRequestParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(PutRequestParser.class);

    private final PutRequestContext context;

    public PutRequestParser(PutRequestContext context) {
        this.context = context;
    }

    public void process() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(context.getMetaModelProcessor(), context.getRequestURI());
        parser.parse();
        context.setParser(parser);

        Gson gson = CommonUtil.getGson();
        JsonElement jsonElement = gson.fromJson(context.getRequest().getReader(), JsonElement.class);
        if (jsonElement == null) {
            throw new BadRequestException("no content to process");
        }
        JsonObject resourceFields = jsonElement.getAsJsonObject();
        if (!parser.isOnlyIdentifiersPresent()) {
            throw new BadRequestException("Primary Key Incomplete.");
        }
        EntityMetadata entityMetadata = parser.getResourceMetadata();

        Object pk = MetadataUtil.createIdentifierFromParser(parser);
        Object entityInstance = context.getEntityManager().find(entityMetadata.getEntityClass(), pk);
        MetadataUtil.updateEntityFromJSON(entityMetadata, context.getEntityManager(), resourceFields, entityInstance);
        context.setEntityInstance(entityInstance);
        LOGGER.info(" test {}", entityInstance);
    }

}
