package com.github.silent.samurai.speedy.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.BooleanCondition;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyQueryImpl;
import com.github.silent.samurai.speedy.models.conditions.BooleanConditionImpl;

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

    BinaryCondition captureSingleBinaryQuery(String fieldName, JsonNode fieldNode) throws SpeedyHttpException {
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

    BooleanCondition createAndCondition(ObjectNode objectNode) throws SpeedyHttpException {
        BooleanCondition booleanCondition = new BooleanConditionImpl(ConditionOperator.AND);
        for (Iterator<String> it2 = objectNode.fieldNames(); it2.hasNext(); ) {
            String fieldName = it2.next();
            JsonNode fieldNode = objectNode.get(fieldName);
            BinaryCondition binaryCondition = captureSingleBinaryQuery(fieldName, fieldNode);
            booleanCondition.addSubCondition(binaryCondition);
        }
        return booleanCondition;
    }

    BooleanCondition createBooleanCondition(ObjectNode jsonNode) throws SpeedyHttpException {
        if (jsonNode.hasNonNull("$or")) {
            BooleanCondition booleanCondition = new BooleanConditionImpl(ConditionOperator.OR);
            JsonNode conditionNode = jsonNode.get("$or");
            if (conditionNode.isArray()) {
                for (JsonNode node : conditionNode) {
                    if (node.isObject()) {
                        BooleanCondition andCondition = createBooleanCondition((ObjectNode) node);
                        booleanCondition.addSubCondition(andCondition);
                    } else {
                        throw new BadRequestException("Invalid query ");
                    }
                }
                return booleanCondition;
            }
            if (conditionNode.isObject()) {
                return createAndCondition((ObjectNode) conditionNode);
            }
        } else if (jsonNode.hasNonNull("$and")) {
            JsonNode conditionNode = jsonNode.get("$and");
            if (conditionNode.isArray()) {
                BooleanCondition booleanCondition = new BooleanConditionImpl(ConditionOperator.AND);
                for (JsonNode node : conditionNode) {
                    if (node.isObject()) {
                        BooleanCondition andCondition = createBooleanCondition((ObjectNode) node);
                        booleanCondition.addSubCondition(andCondition);
                    }
                }
                return booleanCondition;
            }
            if (conditionNode.isObject()) {
                return createAndCondition((ObjectNode) conditionNode);
            }
        }
        return createAndCondition(jsonNode);
    }

    void buildWhere() throws SpeedyHttpException {
        if (rootNode.has("where")) {
            JsonNode jsonNode = rootNode.get("where");
            if (jsonNode.isObject()) {
                speedyQuery.setWhere(createBooleanCondition((ObjectNode) jsonNode));
                return;
            }
            throw new BadRequestException("where must be an object");
        }
    }

    public SpeedyQuery build() throws SpeedyHttpException {
        buildWhere();
        return speedyQuery;
    }
}
