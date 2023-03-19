package com.github.silent.samurai.request.post;


import com.github.silent.samurai.AntlrParser;
import com.github.silent.samurai.Request;
import com.github.silent.samurai.helpers.MetadataUtil;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.utils.CommonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PostRequestParser {
    Logger logger = LogManager.getLogger(PostRequestParser.class);

    private final PostRequestContext context;

    public PostRequestParser(PostRequestContext context) {
        this.context = context;
    }

    public void processBatch() throws Exception {
        AntlrParser antlrParser = new AntlrParser(context.getRequestURI());
        Request request = antlrParser.parse();
        String resource = request.getResource();
        EntityMetadata entityMetadata = context.getMetaModelProcessor().findEntityMetadata(resource);
        context.setResource(resource);
        context.setEntityMetadata(entityMetadata);

        Gson gson = CommonUtil.getGson();
        JsonElement jsonElement = gson.fromJson(context.getHttpServletRequest().getReader(), JsonElement.class);
        JsonArray batchOfEntities = jsonElement.getAsJsonArray();
        for (JsonElement element : batchOfEntities) {
            Object entityInstance = MetadataUtil.getObject(entityMetadata, element.getAsJsonObject());
            logger.info("parsed entity {}", entityInstance);
            context.getParsedObjects().add(entityInstance);
        }
    }

}
