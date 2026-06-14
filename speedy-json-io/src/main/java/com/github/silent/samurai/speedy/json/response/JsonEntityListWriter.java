package com.github.silent.samurai.speedy.json.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.json.registry.JsonRegistry;
import com.github.silent.samurai.speedy.json.walker.SpeedyToJson;
import com.github.silent.samurai.speedy.models.SpeedyEntityResponse;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import jakarta.servlet.http.HttpServletResponse;

/// Writes a paginated list of entities as JSON. One concern: {@code ENTITY_LIST} responses.
public class JsonEntityListWriter {

    private final JsonRegistry jsonRegistry;

    public JsonEntityListWriter(JsonRegistry jsonRegistry) {
        this.jsonRegistry = jsonRegistry;
    }

    public void write(SpeedyEntityResponse entityResponse, HttpServletResponse httpResponse) throws SpeedyHttpException {
        SpeedyToJson speedyToJson = new SpeedyToJson(
                entityResponse.getFieldPredicate(),
                jsonRegistry
        );

        JsonNode jsonElement = speedyToJson.formCollection(
                entityResponse.getPayload(),
                entityResponse.getEntityMetadata(),
                entityResponse.getExpands()
        );

        ObjectMapper json = CommonUtil.json();
        ObjectNode basePayload = json.createObjectNode();
        basePayload.set("payload", jsonElement);
        basePayload.put("pageIndex", entityResponse.getPageIndex());
        basePayload.put("pageSize", entityResponse.getPayload().size());
        if (entityResponse.getTotalCount() != null) {
            basePayload.put("totalCount", entityResponse.getTotalCount());
            basePayload.put("totalPages", calculateTotalPages(entityResponse));
        }

        JsonHttpWriter.writeJson(entityResponse, basePayload, httpResponse);
    }

    private int calculateTotalPages(SpeedyEntityResponse entityResponse) {
        int requestedPageSize = entityResponse.getRequestedPageSize();
        if (requestedPageSize <= 0) {
            return 1;
        }
        long tc = entityResponse.getTotalCount().longValue();
        return (int) Math.ceil((double) tc / requestedPageSize);
    }
}
