package com.github.silent.samurai.request.post;


import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.helpers.MetadataUtil;
import com.github.silent.samurai.parser.SpeedyUriParser;
import com.github.silent.samurai.utils.CommonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostRequestParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostRequestParser.class);

    private final PostRequestContext context;

    public PostRequestParser(PostRequestContext context) {
        this.context = context;
    }

    public void processBatch() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(context.getMetaModelProcessor(), context.getRequestURI());
        parser.parse();
        context.setParser(parser);

        Gson gson = CommonUtil.getGson();
        JsonElement jsonElement = gson.fromJson(context.getRequest().getReader(), JsonElement.class);
        if (jsonElement == null) {
            throw new BadRequestException("no content to process");
        }
        JsonArray batchOfEntities = jsonElement.getAsJsonArray();
        for (JsonElement element : batchOfEntities) {
            Object entityInstance = MetadataUtil.createEntityFromJSON(
                    parser.getResourceMetadata(),
                    element.getAsJsonObject(),
                    context.getEntityManager()
            );
            LOGGER.info("parsed entity {}", entityInstance);
            context.getParsedObjects().add(entityInstance);
        }
    }

}
