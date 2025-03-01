package com.github.silent.samurai.speedy.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.BooleanCondition;
import com.github.silent.samurai.speedy.interfaces.query.QueryField;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyCollection;
import com.github.silent.samurai.speedy.models.SpeedyQueryImpl;
import com.github.silent.samurai.speedy.models.conditions.BooleanConditionImpl;
import com.github.silent.samurai.speedy.parser.ConditionFactory;
import com.github.silent.samurai.speedy.utils.SpeedyValueFactory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Json2SpeedyQueryBuilder {

    final MetaModel metaModel;
    final JsonNode rootNode;
    final SpeedyQueryImpl speedyQuery;

    final ConditionFactory conditionFactory;

    public Json2SpeedyQueryBuilder(MetaModel metaModel, String from, JsonNode rootNode) throws BadRequestException, NotFoundException {
        this.metaModel = metaModel;
        this.rootNode = rootNode;
        this.speedyQuery = new SpeedyQueryImpl(metaModel.findEntityMetadata(from));
        this.conditionFactory = speedyQuery.getConditionFactory();
    }

    public Json2SpeedyQueryBuilder(MetaModel metaModel, EntityMetadata entityMetadata, JsonNode rootNode) throws BadRequestException, NotFoundException {
        this.metaModel = metaModel;
        this.rootNode = rootNode;
        this.speedyQuery = new SpeedyQueryImpl(entityMetadata);
        this.conditionFactory = speedyQuery.getConditionFactory();
    }

    public Json2SpeedyQueryBuilder(MetaModel metaModel, JsonNode rootNode) throws BadRequestException, NotFoundException {
        this.metaModel = metaModel;
        this.rootNode = rootNode;
        this.speedyQuery = new SpeedyQueryImpl(metaModel.findEntityMetadata(getFrom()));
        this.conditionFactory = speedyQuery.getConditionFactory();
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
        // if equals short-handed a : b
        if (fieldNode.isValueNode()) {
            SpeedyValue speedyValue = SpeedyValueFactory.fromJsonValue(queryField.getMetadataForParsing(), (ValueNode) fieldNode);
            return conditionFactory.createBiCondition(queryField, ConditionOperator.EQ, speedyValue);
        }
        // if operator is provided a : { $eq : b }
        if (fieldNode.isObject()) {
            //  fieldNode = { $eq : b }
            for (Iterator<String> it2 = fieldNode.fieldNames(); it2.hasNext(); ) {
                // capture operator
                String operatorSymbol = it2.next();
                // parse operator
                ConditionOperator operator = ConditionOperator.fromSymbol(operatorSymbol);
                // capture value node after operator
                JsonNode valueNode = fieldNode.get(operatorSymbol);

                // if operator is $in or $nin and value is an array, a : { $in : [b, c] }
                if (operator.doesAcceptMultipleValues() && valueNode.isArray()) {
                    List<SpeedyValue> speedyValueList = new LinkedList<>();
                    for (JsonNode node : valueNode) {
                        if (node.isValueNode()) {
                            SpeedyValue speedyValue = SpeedyValueFactory.fromJsonValue(queryField.getMetadataForParsing(), (ValueNode) node);
                            speedyValueList.add(speedyValue);
                        }
                    }
                    SpeedyCollection fieldValue = SpeedyValueFactory.fromCollection(speedyValueList);
                    return conditionFactory.createBiCondition(queryField, operator, fieldValue);
                }
                // if operator is $eq $lte $matches , value will be basic a : { $matches : "sup*" }
                if (valueNode.isValueNode()) {
                    SpeedyValue speedyValue = SpeedyValueFactory.fromJsonValue(queryField.getMetadataForParsing(), (ValueNode) valueNode);
                    return conditionFactory.createBiCondition(queryField, operator, speedyValue);
                }
            }
        }
        throw new BadRequestException("Invalid query");
    }

    BooleanCondition createAndCondition(ObjectNode objectNode) throws SpeedyHttpException {
        // create $and condition from object: { a : { $eq : b }, c : d }
        BooleanCondition booleanCondition = new BooleanConditionImpl(ConditionOperator.AND);
        for (Iterator<String> it2 = objectNode.fieldNames(); it2.hasNext(); ) {
            String fieldName = it2.next();
            JsonNode fieldNode = objectNode.get(fieldName);
            // create a sub query on above $and query
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
                        throw new BadRequestException("order by should be fieldname: asc|desc");
                    }
                }
            }
        }
    }

    void buildPaging() {
        if (rootNode.has("$page")) {
            JsonNode jsonNode = rootNode.get("$page");
            if (jsonNode.isObject()) {
                if (jsonNode.hasNonNull("$index")) {
                    speedyQuery.addPageNo(jsonNode.get("$index").asInt());
                }
                if (jsonNode.hasNonNull("$size")) {
                    speedyQuery.addPageSize(jsonNode.get("$size").asInt());
                }
            }
        }
    }

    void buildExpand() {
        if (rootNode.has("$expand")) {
            JsonNode jsonNode = rootNode.get("$expand");
            if (jsonNode.isArray()) {
                for (JsonNode node : jsonNode) {
                    if (node.isTextual()) {
                        speedyQuery.addExpand(node.asText());
                    }
                }
            }
        }
    }

    void buildSelect() {
        if (rootNode.has("$select")) {
            JsonNode jsonNode = rootNode.get("$select");
            if (jsonNode.isArray()) {
                for (JsonNode node : jsonNode) {
                    if (node.isTextual()) {
                        speedyQuery.addSelect(node.asText());
                    }
                }
            } else if (jsonNode.isTextual()) {
                speedyQuery.addSelect(jsonNode.asText());
            }
        }
    }

    public SpeedyQuery build() throws SpeedyHttpException {
        buildSelect();
        buildWhere();
        buildOrderBy();
        buildPaging();
        buildExpand();
        return speedyQuery;
    }
}
