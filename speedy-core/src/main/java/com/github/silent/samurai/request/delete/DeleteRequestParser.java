package com.github.silent.samurai.request.delete;

import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.helpers.MetadataUtil;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.parser.SpeedyUriParser;
import com.github.silent.samurai.utils.CommonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteRequestParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteRequestParser.class);

    private final DeleteRequestContext context;

    public DeleteRequestParser(DeleteRequestContext context) {
        this.context = context;
    }

    public void process() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(context.getMetaModelProcessor(), context.getRequestURI());
        parser.parse();
        context.setParser(parser);
        EntityMetadata resourceMetadata = parser.getResourceMetadata();

        Gson gson = CommonUtil.getGson();
        JsonElement jsonElement = gson.fromJson(context.getRequest().getReader(), JsonElement.class);
        if (jsonElement == null || !jsonElement.isJsonArray()) {
            throw new BadRequestException("in-valid request");
        }
        JsonArray batchOfEntities = jsonElement.getAsJsonArray();
        for (JsonElement element : batchOfEntities) {
            if (!MetadataUtil.isPrimaryKeyComplete(resourceMetadata, element.getAsJsonObject().keySet())) {
                throw new BadRequestException("Primary Key Incomplete ");
            }
            Object pk = MetadataUtil.createIdentifierFromJSON(resourceMetadata, element.getAsJsonObject());
            Object entityInstance = context.getEntityManager().find(resourceMetadata.getEntityClass(), pk);
            if (entityInstance == null) {
                throw new BadRequestException("entity not found");
            }
            context.getObjectsToBeRemoved().add(entityInstance);
            LOGGER.info("parsed primary key {}", pk);
        }
    }

}
