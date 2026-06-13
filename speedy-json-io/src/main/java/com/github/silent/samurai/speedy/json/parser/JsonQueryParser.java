package com.github.silent.samurai.speedy.json.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.*;
import com.github.silent.samurai.speedy.json.walker.JsonToSpeedy;
import com.github.silent.samurai.speedy.parser.ConditionFactory;
import com.github.silent.samurai.speedy.models.SpeedyBoolean;
import com.github.silent.samurai.speedy.models.SpeedyCollection;
import com.github.silent.samurai.speedy.models.SpeedyQueryImpl;
import com.github.silent.samurai.speedy.models.conditions.BooleanConditionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class JsonQueryParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonQueryParser.class);

    final MetaModel metaModel;
    final JsonNode rootNode;
    final SpeedyQueryImpl speedyQuery;
    final ConditionFactory conditionFactory;

    private int defaultPageSize = 20;
    private JsonToSpeedy jsonToSpeedy;

    public JsonQueryParser(MetaModel metaModel, String from, JsonNode rootNode) throws NotFoundException {
        this.metaModel = metaModel;
        this.rootNode = rootNode;
        this.speedyQuery = new SpeedyQueryImpl(metaModel.findEntityMetadata(from));
        this.conditionFactory = speedyQuery.getConditionFactory();
    }

    public JsonQueryParser(MetaModel metaModel, EntityMetadata entityMetadata, JsonNode rootNode) throws BadRequestException, NotFoundException {
        this.metaModel = metaModel;
        this.rootNode = rootNode;
        this.speedyQuery = new SpeedyQueryImpl(entityMetadata);
        this.conditionFactory = speedyQuery.getConditionFactory();
    }

    public JsonQueryParser(MetaModel metaModel, JsonNode rootNode) throws BadRequestException, NotFoundException {
        this.metaModel = metaModel;
        this.rootNode = rootNode;
        this.speedyQuery = new SpeedyQueryImpl(metaModel.findEntityMetadata(getFrom()));
        this.conditionFactory = speedyQuery.getConditionFactory();
    }

    public void setJsonNode2SpeedyValue(JsonToSpeedy jsonToSpeedy) {
        this.jsonToSpeedy = jsonToSpeedy;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.speedyQuery.setMaxPageSize(maxPageSize);
    }

    public void setDefaultPageSize(int defaultPageSize) {
        this.defaultPageSize = defaultPageSize;
    }

    String getFrom() throws BadRequestException {
        if (rootNode.has("$from")) {
            JsonNode jsonNode = rootNode.get("$from");
            if (jsonNode.isTextual()) {
                return jsonNode.asText();
            }
        }
        throw new BadRequestException("$from must be a string");
    }

    BinaryCondition captureSingleBinaryQuery(String fieldName, JsonNode fieldNode) throws SpeedyHttpException {
        QueryField queryField = speedyQuery.getConditionFactory().createQueryField(fieldName);
        LOGGER.debug("captureSingleBinaryQuery field={}, nodeType={}", fieldName, fieldNode.getNodeType());
        if (fieldNode.isValueNode()) {
            LOGGER.debug("captureSingleBinaryQuery field={}, shorthand value node", fieldName);
            if (fieldNode.isTextual()) {
                String text = fieldNode.asText();
                if ("$isnull".equals(text)) {
                    return conditionFactory.createBiCondition(queryField, ConditionOperator.ISNULL, new Literal(new SpeedyBoolean(true)));
                }
                if ("$isnotnull".equals(text)) {
                    return conditionFactory.createBiCondition(queryField, ConditionOperator.ISNOTNULL, new Literal(new SpeedyBoolean(true)));
                }
            }
            Expression expression = buildExpression(queryField.getMetadataForParsing(), (ValueNode) fieldNode);
            return conditionFactory.createBiCondition(queryField, ConditionOperator.EQ, expression);
        }
        if (fieldNode.isObject()) {
            for (Iterator<String> it2 = fieldNode.fieldNames(); it2.hasNext(); ) {
                String operatorSymbol = it2.next();
                ConditionOperator operator = ConditionOperator.fromSymbol(operatorSymbol);
                JsonNode valueNode = fieldNode.get(operatorSymbol);
                LOGGER.debug("captureSingleBinaryQuery field={}, operator={}, valueType={}, isArray={}, isValue={}",
                        fieldName, operatorSymbol, valueNode.getNodeType(),
                        valueNode.isArray(), valueNode.isValueNode());

                if (operator.doesAcceptMultipleValues() && valueNode.isArray()) {
                    List<SpeedyValue> speedyValueList = new LinkedList<>();
                    for (JsonNode node : valueNode) {
                        if (node.isValueNode()) {
                            SpeedyValue speedyValue = jsonToSpeedy.fromValueNode(queryField.getMetadataForParsing(), (ValueNode) node);
                            speedyValueList.add(speedyValue);
                        }
                    }
                    SpeedyCollection fieldValue = new SpeedyCollection(speedyValueList);
                    return conditionFactory.createBiCondition(queryField, operator, new Literal(fieldValue));
                }
                if (operator == ConditionOperator.ISNULL || operator == ConditionOperator.ISNOTNULL) {
                    if (!valueNode.isBoolean()) {
                        throw new BadRequestException("$" + operator.name().toLowerCase()
                                + " only accepts a boolean value");
                    }
                    return conditionFactory.createBiCondition(queryField, operator,
                            new Literal(new SpeedyBoolean(valueNode.asBoolean())));
                }
                if (valueNode.isValueNode()) {
                    Expression expression = buildExpression(queryField.getMetadataForParsing(), (ValueNode) valueNode);
                    return conditionFactory.createBiCondition(queryField, operator, expression);
                }
            }
        }
        LOGGER.debug("captureSingleBinaryQuery throwing Invalid query for field={}, nodeType={}",
                fieldName, fieldNode.getNodeType());
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

    Expression buildExpression(FieldMetadata metadata, ValueNode symbol) throws SpeedyHttpException {
        if (symbol.isTextual() && symbol.asText().startsWith("$")) {
            String field = symbol.asText().substring(1);
            QueryField queryField = this.conditionFactory.createQueryField(field);
            this.conditionFactory.validateQueryFieldNotSensitive(queryField);
            return new Identifier(queryField);
        } else {
            return new Literal(jsonToSpeedy.fromValueNode(metadata, symbol));
        }
    }

    void buildWhere() throws SpeedyHttpException {
        if (rootNode.has("$where")) {
            JsonNode jsonNode = rootNode.get("$where");
            if (jsonNode.isObject()) {
                speedyQuery.setWhere(createBooleanCondition((ObjectNode) jsonNode));
                return;
            }
            throw new BadRequestException("$where must be an object");
        }
    }

    void buildOrderBy() throws NotFoundException, BadRequestException {
        if (rootNode.has("$orderBy")) {
            JsonNode jsonNode = rootNode.get("$orderBy");
            if (jsonNode.isObject()) {
                for (Iterator<String> it2 = jsonNode.fieldNames(); it2.hasNext(); ) {
                    String fieldName = it2.next();
                    JsonNode fieldNode = jsonNode.get(fieldName);
                    if (fieldNode.isTextual() && fieldNode.asText().equalsIgnoreCase("ASC")) {
                        speedyQuery.orderByAsc(fieldName);
                    } else if (fieldNode.isTextual() && fieldNode.asText().equalsIgnoreCase("DESC")) {
                        speedyQuery.orderByDesc(fieldName);
                    } else {
                        throw new BadRequestException("order by should be field name: asc|desc");
                    }
                }
            }
        }
    }

    void buildPaging() throws BadRequestException {
        if (rootNode.has("$page")) {
            JsonNode jsonNode = rootNode.get("$page");
            if (jsonNode.isObject()) {
                if (jsonNode.hasNonNull("$index")) {
                    speedyQuery.addPageNo(jsonNode.get("$index").asInt());
                }
                if (jsonNode.hasNonNull("$size")) {
                    int pageSize = jsonNode.get("$size").asInt();
                    if (pageSize > speedyQuery.getMaxPageSize()) {
                        throw new BadRequestException(
                                "Requested page size " + pageSize + " exceeds maximum allowed page size " + speedyQuery.getMaxPageSize()
                        );
                    }
                    speedyQuery.addPageSize(pageSize);
                }
            }
        }
    }

    void buildExpand() throws SpeedyHttpException {
        if (rootNode.has("$expand")) {
            JsonNode jsonNode = rootNode.get("$expand");
            if (jsonNode.isArray()) {
                for (JsonNode node : jsonNode) {
                    if (node.isTextual()) {
                        String expansion = node.asText();
                        validateExpansionPath(expansion);
                        speedyQuery.addExpand(expansion);
                    }
                }
            }
        }
    }

    private void validateExpansionPath(String expansion) throws BadRequestException {
        if (expansion.contains(".")) {
            String[] parts = expansion.split("\\.");
            if (parts.length < 2) {
                throw new BadRequestException("Invalid expansion path: " + expansion);
            }
            for (String part : parts) {
                if (part.trim().isEmpty()) {
                    throw new BadRequestException("Invalid expansion path: " + expansion + " - empty segment");
                }
            }
        }
    }

    void buildSelect() throws BadRequestException {
        if (rootNode.has("$select")) {
            JsonNode jsonNode = rootNode.get("$select");
            if (jsonNode.isArray()) {
                for (JsonNode node : jsonNode) {
                    if (node.isTextual()) {
                        if ("$count".equals(node.asText())) {
                            speedyQuery.setCountRequest(true);
                        } else {
                            speedyQuery.addSelect(node.asText());
                        }
                    }
                }
            } else if (jsonNode.isTextual()) {
                if ("$count".equals(jsonNode.asText())) {
                    speedyQuery.setCountRequest(true);
                } else {
                    speedyQuery.addSelect(jsonNode.asText());
                }
            }
            if (speedyQuery.isCountRequest() && !speedyQuery.getSelect().isEmpty()) {
                throw new BadRequestException(
                        "$select cannot mix '$count' with field names. Use '$count' alone to request a count.");
            }
        }
    }

    public SpeedyQuery build() throws SpeedyHttpException {
        buildSelect();
        buildWhere();
        buildOrderBy();
        speedyQuery.addPageSize(Math.min(defaultPageSize, speedyQuery.getMaxPageSize()));
        buildPaging();
        buildExpand();
        return speedyQuery;
    }
}
