package com.github.silent.samurai.speedy.serializers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.io.SelectiveSpeedy2Json;
import com.github.silent.samurai.speedy.models.*;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;

public class JSONSerializerV2 implements IResponseSerializerV2 {

    private final MetaModel metaModel;
    private final EntityMetadata entityMetadata;

    public JSONSerializerV2(MetaModel metaModel, EntityMetadata entityMetadata) {
        this.metaModel = metaModel;
        this.entityMetadata = entityMetadata;
    }

    @Override
    public String getContentType() {
        return MediaType.APPLICATION_JSON_VALUE;
    }

    @Override
    public void writeEntityList(SpeedyEntityResponse entityResponse, HttpServletResponse httpResponse)
            throws SpeedyHttpException {

        SelectiveSpeedy2Json selectiveSpeedy2Json = new SelectiveSpeedy2Json(
                metaModel,
                entityResponse.getFieldPredicate()
        );

        JsonNode jsonElement = selectiveSpeedy2Json.formCollection(
                entityResponse.getPayload(),
                entityMetadata,
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

        httpResponse.setContentType(this.getContentType());
        httpResponse.setStatus(entityResponse.getStatus());
        entityResponse.getHeaders().forEach(httpResponse::setHeader);

        try {
            json.writeValue(httpResponse.getWriter(), basePayload);
        } catch (IOException e) {
            throw new InternalServerError("Internal Server Error", e);
        }
    }

    @Override
    public void writeCount(SpeedyCountResponse countResponse, HttpServletResponse httpResponse)
            throws SpeedyHttpException {
        try {
            httpResponse.setContentType(this.getContentType());
            httpResponse.setStatus(countResponse.getStatus());
            countResponse.getHeaders().forEach(httpResponse::setHeader);

            ObjectMapper json = CommonUtil.json();
            ObjectNode basePayload = json.createObjectNode();
            basePayload.set("count", json.valueToTree(countResponse.getCount()));
            json.writeValue(httpResponse.getWriter(), basePayload);
        } catch (IOException e) {
            throw new InternalServerError("Internal Server Error", e);
        }
    }

    @Override
    public void writeBatch(SpeedyBatchResponse batchResponse, HttpServletResponse httpResponse)
            throws SpeedyHttpException {
        ObjectMapper mapper = CommonUtil.json();

        httpResponse.setStatus(batchResponse.getStatus());
        httpResponse.setContentType(this.getContentType());
        batchResponse.getHeaders().forEach(httpResponse::setHeader);

        try {
            ObjectNode root = mapper.createObjectNode();

            ArrayNode succeededArray = root.putArray("succeeded");
            for (SpeedyEntity entity : batchResponse.getSucceeded()) {
                succeededArray.add(serializeEntityKeys(mapper, entity));
            }

            ArrayNode failedArray = root.putArray("failed");
            for (SpeedyPartialFailure f : batchResponse.getFailed()) {
                ObjectNode failureNode = failedArray.addObject();
                failureNode.put("index", f.getIndex());
                failureNode.put("status", f.getStatus());
                failureNode.put("message", f.getMessage());
                failureNode.put("timestamp", f.getTimestamp());
                if (f.getInputPk() != null) {
                    failureNode.putPOJO("inputPk", serializeEntityKeys(mapper, f.getInputPk()));
                } else {
                    failureNode.putNull("inputPk");
                }
            }

            root.put("pageIndex", batchResponse.getPageIndex());
            mapper.writeValue(httpResponse.getWriter(), root);
        } catch (IOException e) {
            throw new InternalServerError("Internal Server Error", e);
        }
    }

    private ObjectNode serializeEntityKeys(ObjectMapper mapper, SpeedyEntity entity) {
        ObjectNode node = mapper.createObjectNode();
        for (KeyFieldMetadata keyField : entity.getMetadata().getKeyFields()) {
            node.putPOJO(keyField.getOutputPropertyName(),
                    serializeSpeedyValue(entity.get(keyField)));
        }
        return node;
    }

    private Object serializeSpeedyValue(SpeedyValue value) {
        if (value == null || value.isNull()) return null;
        if (value instanceof SpeedyInt si) return si.asInt();
        if (value instanceof SpeedyDouble sd) return sd.asDouble();
        if (value instanceof SpeedyBoolean sb) return sb.asBoolean();
        return value.asText();
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
