package com.github.silent.samurai.request.delete;

import com.github.silent.samurai.AntlrParser;
import com.github.silent.samurai.AntlrRequest;
import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.helpers.MetadataUtil;
import com.github.silent.samurai.interfaces.EntityMetadata;
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
        AntlrParser antlrParser = new AntlrParser(context.getRequestURI());
        AntlrRequest antlrRequest = antlrParser.parse();
        String resource = antlrRequest.getResource();
        EntityMetadata entityMetadata = context.getMetaModelProcessor().findEntityMetadata(resource);
        context.setResource(resource);
        context.setEntityMetadata(entityMetadata);

        Gson gson = CommonUtil.getGson();
        JsonElement jsonElement = gson.fromJson(context.getRequest().getReader(), JsonElement.class);
        if (jsonElement == null) {
            throw new BadRequestException("no content to process");
        }
        JsonArray batchOfEntities = jsonElement.getAsJsonArray();
        for (JsonElement element : batchOfEntities) {
            if (!MetadataUtil.isPrimaryKeyComplete(entityMetadata, element.getAsJsonObject().keySet())) {
                throw new BadRequestException("Primary Key Incomplete ");
            }
            Object pk = MetadataUtil.createIdentifierFromJSON(entityMetadata, element.getAsJsonObject());
            Object entityInstance = context.getEntityManager().find(entityMetadata.getEntityClass(), pk);
            context.getObjectsToBeRemoved().add(entityInstance);
            LOGGER.info("parsed primary key {}", pk);
        }
    }

}
