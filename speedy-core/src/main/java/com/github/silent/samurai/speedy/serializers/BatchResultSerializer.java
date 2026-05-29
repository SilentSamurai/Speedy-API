package com.github.silent.samurai.speedy.serializers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.models.SpeedyBoolean;
import com.github.silent.samurai.speedy.models.SpeedyDouble;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyInt;
import com.github.silent.samurai.speedy.models.SpeedyPartialFailure;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

public class BatchResultSerializer implements IResponseSerializerV2 {

    private final List<SpeedyEntity> succeeded;
    private final List<SpeedyPartialFailure> failures;
    private final int pageIndex;
    private final ObjectMapper mapper;

    public BatchResultSerializer(List<SpeedyEntity> succeeded,
                                  List<SpeedyPartialFailure> failures,
                                  int pageIndex) {
        this.succeeded = succeeded;
        this.failures = failures;
        this.pageIndex = pageIndex;
        this.mapper = new ObjectMapper();
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public void write(IResponseContext context) throws com.github.silent.samurai.speedy.exceptions.SpeedyHttpException {
        HttpServletResponse response = context.getResponse();
        boolean hasFailures = failures != null && !failures.isEmpty();
        boolean hasSuccess = succeeded != null && !succeeded.isEmpty();

        int status;
        if (hasFailures && !hasSuccess) {
            status = HttpServletResponse.SC_BAD_REQUEST;
        } else if (hasFailures) {
            status = 207;
        } else {
            status = HttpServletResponse.SC_OK;
        }
        response.setStatus(status);
        response.setContentType("application/json");

        try {
            ObjectNode root = mapper.createObjectNode();

            ArrayNode succeededArray = root.putArray("succeeded");
            for (SpeedyEntity entity : succeeded) {
                succeededArray.add(serializeEntityKeys(entity));
            }

            ArrayNode failedArray = root.putArray("failed");
            if (failures != null) {
                for (SpeedyPartialFailure f : failures) {
                    ObjectNode failureNode = failedArray.addObject();
                    failureNode.put("index", f.getIndex());
                    failureNode.put("status", f.getStatus());
                    failureNode.put("message", f.getMessage());
                    failureNode.put("timestamp", f.getTimestamp());
                    if (f.getInputPk() != null) {
                        failureNode.putPOJO("inputPk", serializeEntityKeys(f.getInputPk()));
                    } else {
                        failureNode.putNull("inputPk");
                    }
                }
            }

            root.put("pageIndex", pageIndex);
            mapper.writeValue(response.getWriter(), root);
        } catch (IOException e) {
            throw new com.github.silent.samurai.speedy.exceptions.InternalServerError("Internal Server Error", e);
        }
    }

    private ObjectNode serializeEntityKeys(SpeedyEntity entity) {
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
}
