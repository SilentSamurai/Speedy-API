package com.github.silent.samurai.speedy.json.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.json.registry.JsonRegistry;
import com.github.silent.samurai.speedy.models.SpeedyBatchResponse;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyPartialFailure;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import jakarta.servlet.http.HttpServletResponse;

/// Writes a batch result (succeeded keys + per-item failures) as JSON.
/// One concern: {@code BATCH_RESULT} responses.
public class JsonBatchWriter {

    private final JsonRegistry jsonRegistry;

    public JsonBatchWriter(JsonRegistry jsonRegistry) {
        this.jsonRegistry = jsonRegistry;
    }

    public void write(SpeedyBatchResponse batchResponse, HttpServletResponse httpResponse) throws SpeedyHttpException {
        ObjectMapper mapper = CommonUtil.json();
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
        JsonHttpWriter.writeJson(batchResponse, root, httpResponse);
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
        Object encoded = jsonRegistry.encode(value.getValueType(), value);
        if (encoded != null) return encoded;
        return value.asText();
    }
}
