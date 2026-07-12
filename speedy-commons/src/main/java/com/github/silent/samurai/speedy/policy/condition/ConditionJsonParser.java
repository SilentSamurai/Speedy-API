package com.github.silent.samurai.speedy.policy.condition;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.BooleanCondition;
import com.github.silent.samurai.speedy.interfaces.query.Condition;
import com.github.silent.samurai.speedy.interfaces.query.Expression;
import com.github.silent.samurai.speedy.interfaces.query.Identifier;
import com.github.silent.samurai.speedy.interfaces.query.Literal;
import com.github.silent.samurai.speedy.interfaces.query.QueryField;
import com.github.silent.samurai.speedy.interfaces.query.VariableRef;
import com.github.silent.samurai.speedy.models.SpeedyBoolean;
import com.github.silent.samurai.speedy.models.SpeedyCollection;
import com.github.silent.samurai.speedy.models.conditions.BooleanConditionImpl;
import com.github.silent.samurai.speedy.parser.ConditionFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConditionJsonParser {

    private static final Pattern PRINCIPAL_REF = Pattern.compile("^\\$\\{([^}]+)}$");

    private ConditionJsonParser() {
    }

    public static BooleanCondition parse(Map<String, Object> conditionMap, EntityMetadata entityMetadata)
            throws SpeedyHttpException {
        ConditionFactory cf = new ConditionFactory(entityMetadata);
        return parseObject(conditionMap, cf);
    }

    @SuppressWarnings("unchecked")
    private static BooleanCondition parseObject(Map<String, Object> obj, ConditionFactory cf)
            throws SpeedyHttpException {
        String firstLogical = null;
        Object firstLogicalValue = null;
        Map<String, Object> fieldConditions = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : obj.entrySet()) {
            String key = entry.getKey();
            if ("$or".equals(key) || "or".equals(key)) {
                if (!fieldConditions.isEmpty()) {
                    throw new BadRequestException("$or must be the only key of a condition object");
                }
                if (firstLogical != null) {
                    throw new BadRequestException("$or/$and must be the only key of a condition object");
                }
                firstLogical = "or";
                firstLogicalValue = entry.getValue();
            } else if ("$and".equals(key) || "and".equals(key)) {
                if (!fieldConditions.isEmpty()) {
                    throw new BadRequestException("$and must be the only key of a condition object");
                }
                if (firstLogical != null) {
                    throw new BadRequestException("$or/$and must be the only key of a condition object");
                }
                firstLogical = "and";
                firstLogicalValue = entry.getValue();
            } else {
                if (firstLogical != null) {
                    throw new BadRequestException("$or/$and must be the only key of a condition object");
                }
                fieldConditions.put(key, entry.getValue());
            }
        }

        if (firstLogical != null) {
            List<Map<String, Object>> clauses;
            if (firstLogicalValue instanceof List<?> list) {
                clauses = new ArrayList<>();
                for (Object elem : list) {
                    if (!(elem instanceof Map)) {
                        throw new BadRequestException("$or/$and array elements must be objects");
                    }
                    clauses.add((Map<String, Object>) elem);
                }
            } else if (firstLogicalValue instanceof Map) {
                clauses = List.of((Map<String, Object>) firstLogicalValue);
            } else {
                throw new BadRequestException("$or/$and must be an array or object");
            }
            ConditionOperator op = "or".equals(firstLogical) ? ConditionOperator.OR : ConditionOperator.AND;
            BooleanCondition group = new BooleanConditionImpl(op);
            for (Map<String, Object> clause : clauses) {
                group.addSubCondition(parseObject(clause, cf));
            }
            return group;
        }

        BooleanCondition and = new BooleanConditionImpl(ConditionOperator.AND);
        for (Map.Entry<String, Object> entry : fieldConditions.entrySet()) {
            and.addSubCondition(captureFieldCondition(entry.getKey(), entry.getValue(), cf));
        }
        return and;
    }

    @SuppressWarnings("unchecked")
    private static Condition captureFieldCondition(String fieldName, Object value, ConditionFactory cf)
            throws SpeedyHttpException {
        QueryField queryField = cf.createQueryField(fieldName);
        FieldMetadata metadata = queryField.getMetadataForParsing();

        if (value instanceof Map<?, ?> map) {
            Map<String, Object> opMap = (Map<String, Object>) map;
            if (opMap.isEmpty()) {
                throw new BadRequestException("Empty condition object for field '" + fieldName + "'");
            }
            if (opMap.size() == 1 && (opMap.containsKey("$or") || opMap.containsKey("or")
                    || opMap.containsKey("$and") || opMap.containsKey("and"))) {
                String logicalKey = opMap.containsKey("$or") || opMap.containsKey("or") ? "$or" : "$and";
                Object logicalValue = opMap.get(logicalKey);
                if (logicalValue == null) {
                    logicalValue = opMap.get(logicalKey.substring(1));
                }
                return captureFieldLogicalGroup(queryField, metadata, logicalKey, logicalValue, cf);
            }
            if (opMap.size() != 1) {
                throw new BadRequestException(
                        "Field '" + queryField.getMetadataForParsing().getOutputPropertyName()
                                + "' has multiple operators in a single condition object; combine them with $and instead");
            }
            Map.Entry<String, Object> opEntry = opMap.entrySet().iterator().next();
            return captureOperatorCondition(queryField, metadata, opEntry.getKey(), opEntry.getValue(), cf);
        }

        if (value instanceof List<?>) {
            throw new BadRequestException("Unexpected array for field '" + fieldName + "'");
        }

        Expression expression = buildExpression(metadata, value, cf);
        return cf.createBiCondition(queryField, ConditionOperator.EQ, expression);
    }

    @SuppressWarnings("unchecked")
    private static Condition captureFieldLogicalGroup(QueryField queryField, FieldMetadata metadata,
                                                       String logicalKey, Object logicalValue,
                                                        ConditionFactory cf) throws SpeedyHttpException {
        List<Map<String, Object>> clauses;
        if (logicalValue instanceof List<?> list) {
            clauses = new ArrayList<>();
            for (Object elem : list) {
                if (!(elem instanceof Map)) {
                    throw new BadRequestException("$or/$and array elements must be objects");
                }
                clauses.add((Map<String, Object>) elem);
            }
        } else {
            throw new BadRequestException("Field-level $or/$and must be an array");
        }
        ConditionOperator op = logicalKey.contains("or") ? ConditionOperator.OR : ConditionOperator.AND;
        BooleanCondition group = new BooleanConditionImpl(op);
        for (Map<String, Object> clause : clauses) {
            group.addSubCondition(parseObject(clause, cf));
        }
        return group;
    }

    @SuppressWarnings("unchecked")
    private static Condition captureOperatorCondition(QueryField queryField, FieldMetadata metadata,
                                                       String operatorSymbol, Object value,
                                                        ConditionFactory cf) throws SpeedyHttpException {
        ConditionOperator operator = ConditionOperator.fromSymbol(operatorSymbol);

        if (operator == ConditionOperator.ISNULL || operator == ConditionOperator.ISNOTNULL) {
            boolean boolVal = value instanceof Boolean b ? b : false;
            return cf.createBiCondition(queryField, operator, new Literal(new SpeedyBoolean(boolVal)));
        }

        if (operator.doesAcceptMultipleValues() && value instanceof List<?> list) {
            List<SpeedyValue> values = new ArrayList<>();
            for (Object elem : list) {
                if (elem instanceof Map || elem instanceof List) {
                    throw new BadRequestException("$in/$nin/$between values must be scalar");
                }
                values.add(buildValue(elem));
            }
            return cf.createBiCondition(queryField, operator, new Literal(new SpeedyCollection(values)));
        }

        if (operator.doesAcceptMultipleValues()) {
            throw new BadRequestException("$" + operator.name().toLowerCase() + " requires an array of values");
        }

        Expression expression = buildExpression(metadata, value, cf);
        return cf.createBiCondition(queryField, operator, expression);
    }

    private static Expression buildExpression(FieldMetadata metadata, Object value, ConditionFactory cf)
            throws SpeedyHttpException {
        if (value instanceof String s) {
            Matcher variableMatcher = PRINCIPAL_REF.matcher(s.trim());
            if (variableMatcher.matches()) {
                return new VariableRef(variableMatcher.group(1));
            }
            if (s.startsWith("$")) {
                QueryField qf = cf.createQueryField(s.substring(1));
                cf.validateQueryFieldNotSensitive(qf);
                return new Identifier(qf);
            }
        }
        return new Literal(buildValue(value));
    }

    static SpeedyValue buildValue(Object raw) {
        if (raw == null) {
            return OperandValues.wrap(null);
        }
        if (raw instanceof String s) {
            Matcher variableMatcher = PRINCIPAL_REF.matcher(s.trim());
            if (variableMatcher.matches()) {
                return OperandValues.wrap("${" + variableMatcher.group(1) + "}");
            }
        }
        return OperandValues.wrap(raw);
    }
}
