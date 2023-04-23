package com.github.silent.samurai.request.put;

import com.github.silent.samurai.AntlrParser;
import com.github.silent.samurai.Request;
import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.helpers.MetadataUtil;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.utils.CommonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class PutRequestParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(PutRequestParser.class);

    private final PutRequestContext context;

    public PutRequestParser(PutRequestContext context) {
        this.context = context;
    }

    public void process() throws Exception {
        AntlrParser antlrParser = new AntlrParser(context.getRequestURI());
        Request request = antlrParser.parse();
        String resource = request.getResource();
        Map<String, String> keyFields = request.getKeywords();
        EntityMetadata entityMetadata = context.getMetaModelProcessor().findEntityMetadata(resource);
        context.setResource(resource);
        context.setEntityMetadata(entityMetadata);

        Gson gson = CommonUtil.getGson();
        JsonElement jsonElement = gson.fromJson(context.getHttpServletRequest().getReader(), JsonElement.class);
        if (jsonElement == null) {
            throw new BadRequestException("no content to process");
        }
        JsonObject resourceFields = jsonElement.getAsJsonObject();

        if (!MetadataUtil.isPrimaryKeyComplete(entityMetadata, keyFields.keySet())) {
            throw new BadRequestException("Primary Key Incomplete.");
        }

        Object pk = MetadataUtil.createEntityKeyFromMap(keyFields, entityMetadata);
        Object entityInstance = context.getEntityManager().find(entityMetadata.getEntityClass(), pk);
        MetadataUtil.updateEntityFromJson(entityMetadata, context.getEntityManager(), resourceFields, entityInstance);
        context.setEntityInstance(entityInstance);
        LOGGER.info(" test {}", entityInstance);
    }

}
