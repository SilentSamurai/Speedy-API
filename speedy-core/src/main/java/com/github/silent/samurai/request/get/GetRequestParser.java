package com.github.silent.samurai.request.get;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.utils.CommonUtil;

import java.util.Map;

public class GetRequestParser {

    private final GetRequestContext context;

    public GetRequestParser(GetRequestContext context) {
        this.context = context;
    }

    public void process() throws Exception {

        StringBuilder sb = new StringBuilder();
        sb.append(context.getRequestURI());

        int contentLength = context.getRequest().getContentLength();
        JsonNode jsonElement = null;
        if (contentLength > 0) {
            ObjectMapper json = CommonUtil.json();
            jsonElement = json.readTree(context.getRequest().getReader());
            sb.append(parseGetBody(jsonElement));
        }

        SpeedyUriContext parser = new SpeedyUriContext(context.getMetaModelProcessor(), sb.toString());
        parser.parse();
        context.setParser(parser);

        if (contentLength > 0) {
            parseQuery(jsonElement, parser);
        }
    }

    public void parseQuery(JsonNode rootNode, SpeedyUriContext uriContext) {
        if (rootNode.has("pageSize")) {
            int pageSize = rootNode.get("pageSize").asInt();
            if (pageSize <= 0) pageSize = SpeedyConstant.defaultPageSize;
            uriContext.getRawQuery().add("pageSize", String.valueOf(pageSize));
        }
        if (rootNode.has("pageIndex")) {
            int pageIndex = rootNode.get("pageIndex").asInt();
            if (pageIndex < 0) pageIndex = 0;
            uriContext.getRawQuery().add("pageIndex", String.valueOf(pageIndex));
        }
        if (rootNode.has("orderBy") && rootNode.get("orderBy").isArray()) {
            for (JsonNode element : rootNode.get("orderBy")) {
                uriContext.getRawQuery().add("orderBy", element.asText());
            }
        }
        if (rootNode.has("orderByDesc") && rootNode.get("orderByDesc").isArray()) {
            for (JsonNode element : rootNode.get("orderByDesc")) {
                uriContext.getRawQuery().add("orderByDesc", element.asText());
            }
        }
    }

    public String parseGetBody(JsonNode rootNode) throws Exception {
        StringBuilder sb = new StringBuilder();

        if (rootNode.has("where") && !rootNode.get("where").isNull() && !rootNode.get("where").asText().isBlank()) {
            String where = rootNode.get("where").asText();
            sb.append("(").append(where).append(")");
        }

        if (rootNode.has("join") && rootNode.get("join").isObject() && rootNode.get("join").elements().hasNext()) {
            ObjectNode joinNode = (ObjectNode) rootNode.get("join");
            Map.Entry<String, JsonNode> jsonNode = joinNode.fields().next();
            String key = jsonNode.getKey();
            sb.append("/").append(key).append("(").append(jsonNode.getValue().asText()).append(")");
        }

        return sb.toString();
    }

}
