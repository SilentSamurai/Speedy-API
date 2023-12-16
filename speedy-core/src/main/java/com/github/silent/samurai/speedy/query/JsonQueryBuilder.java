package com.github.silent.samurai.speedy.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.BooleanCondition;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyQueryImpl;

import java.util.Iterator;

public class JsonQueryBuilder {

    final MetaModelProcessor metaModelProcessor;
    final JsonNode rootNode;
    final SpeedyQueryImpl speedyQuery;

    public JsonQueryBuilder(MetaModelProcessor metaModelProcessor, JsonNode rootNode) throws BadRequestException, NotFoundException {
        this.metaModelProcessor = metaModelProcessor;
        this.rootNode = rootNode;
        this.speedyQuery = new SpeedyQueryImpl(metaModelProcessor.findEntityMetadata(getFrom()));
    }

    String getFrom() throws BadRequestException {
        rootNode.has("from");
        if (rootNode.has("from")) {
            JsonNode jsonNode = rootNode.get("from");
            if (jsonNode.isTextual()) {
                return jsonNode.asText();
            }
        }
        throw new BadRequestException("from must be a string");
    }

    BinaryCondition createBinaryCondition(String fieldName, String operator, ValueNode fieldNode) throws SpeedyHttpException {
        String associatedField = null;
        if (fieldName.contains(".")) {
            String[] parts = fieldName.split("\\.");
            if (parts.length == 2) {
                fieldName = parts[0];
                associatedField = parts[1];
            }
        }
        if (associatedField != null) {
            return speedyQuery.getConditionFactory()
                    .createAssociatedCondition(fieldName, associatedField, operator, fieldNode);
        }
        return speedyQuery.getConditionFactory()
                .createBinaryCondition(fieldName, operator, fieldNode);
    }

    BinaryCondition captureBinaryQuery(String fieldName, JsonNode fieldNode) throws SpeedyHttpException {
        if (fieldNode.isValueNode()) {
            return createBinaryCondition(fieldName, "=", (ValueNode) fieldNode);
        }
        if (fieldNode.isObject()) {
            for (Iterator<String> it2 = fieldNode.fieldNames(); it2.hasNext(); ) {
                String operatorSymbol = it2.next();
                JsonNode valueNode = fieldNode.get(operatorSymbol);
                if (valueNode.isValueNode()) {
                    return createBinaryCondition(fieldName, operatorSymbol, (ValueNode) valueNode);
                }
            }
        }
        throw new BadRequestException("Invalid query");
    }

    void buildWhere() throws SpeedyHttpException {
        if (rootNode.has("where")) {
            JsonNode jsonNode = rootNode.get("where");
            if (jsonNode.isObject()) {
                BooleanCondition where = speedyQuery.getWhere();
                for (Iterator<String> it = jsonNode.fieldNames(); it.hasNext(); ) {
                    String fieldName = it.next();
                    // filter node
                    JsonNode fieldNode = jsonNode.get(fieldName);
                    BinaryCondition binaryCondition = captureBinaryQuery(fieldName, fieldNode);
                    where.addSubCondition(binaryCondition);
                }
            }
        }
    }

    public SpeedyQuery build() throws SpeedyHttpException {
        buildWhere();
        return speedyQuery;
    }
}
