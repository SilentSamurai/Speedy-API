package com.github.silent.samurai.speedy.serialization;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.request.StructureReader;
import com.github.silent.samurai.speedy.interfaces.request.StructureReader.Kind;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.BooleanCondition;
import com.github.silent.samurai.speedy.interfaces.query.Expression;
import com.github.silent.samurai.speedy.interfaces.query.Identifier;
import com.github.silent.samurai.speedy.interfaces.query.Literal;
import com.github.silent.samurai.speedy.interfaces.query.QueryField;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyBoolean;
import com.github.silent.samurai.speedy.models.SpeedyCollection;
import com.github.silent.samurai.speedy.models.SpeedyQueryImpl;
import com.github.silent.samurai.speedy.models.conditions.BooleanConditionImpl;
import com.github.silent.samurai.speedy.parser.ConditionFactory;

import java.util.LinkedList;
import java.util.List;

/// Format-agnostic {@code $query} parser: owns the whole structural traversal of a query
/// body (the {@code $where} boolean condition tree with {@code $or}/{@code $and}, per-field
/// operator handling, {@code $orderBy}, {@code $page}, {@code $expand}, {@code $select}) and
/// assembles a {@link SpeedyQuery}. It drives a streaming {@link StructureReader} — no
/// document tree — and the only format-specific step is leaf decoding, delegated to
/// {@link StructureReader#readField}. The read-side mirror of {@link SpeedyToStructure} and the
/// query-shaped sibling of {@link StructureToSpeedy}.
///
/// Clauses are dispatched in document order (each writes an independent part of the query),
/// and a condition object treats {@code $or}/{@code $and} as a logical group that must be the
/// **only** key of its object — a single-key lookahead is therefore enough and no per-object
/// buffering is needed.
///
/// Stateless and thread-safe; the same instance can serve every request.
public class StructureToQuery {

    public SpeedyQuery parse(EntityMetadata entity, StructureReader r,
                             int maxPageSize, int defaultPageSize) throws SpeedyHttpException {
        SpeedyQueryImpl query = new SpeedyQueryImpl(entity);
        query.setMaxPageSize(maxPageSize);
        query.addPageSize(Math.min(defaultPageSize, maxPageSize));
        ConditionFactory conditionFactory = query.getConditionFactory();

        if (r.begin() != Kind.OBJECT) {
            throw new BadRequestException("query body must be an object");
        }
        String key;
        while ((key = r.nextKey()) != null) {
            switch (key) {
                case "$from" -> {
                    // The caller supplies the entity; the body value is only validated.
                    if (r.textValue() == null) {
                        throw new BadRequestException("$from must be a string");
                    }
                }
                case "$select" -> buildSelect(query, r);
                case "$where" -> {
                    if (r.currentKind() != Kind.OBJECT) {
                        throw new BadRequestException("$where must be an object");
                    }
                    query.setWhere(parseBoolean(conditionFactory, r));
                }
                case "$orderBy" -> buildOrderBy(query, r);
                case "$page" -> buildPaging(query, r);
                case "$expand" -> buildExpand(query, r);
                default -> r.skipValue();
            }
        }
        query.setType(SpeedyRequestType.QUERY);
        return query;
    }

    /// Reads a condition object (the cursor is positioned on its opening token). Returns a
    /// logical group when the sole key is {@code $or}/{@code $and}, otherwise an {@code AND}
    /// of the field conditions in the object.
    private BooleanCondition parseBoolean(ConditionFactory cf, StructureReader r) throws SpeedyHttpException {
        String first = r.nextKey();
        if (first == null) {
            return new BooleanConditionImpl(ConditionOperator.AND);
        }
        if ("$or".equals(first)) {
            return parseLogicalGroup(ConditionOperator.OR, cf, r);
        }
        if ("$and".equals(first)) {
            return parseLogicalGroup(ConditionOperator.AND, cf, r);
        }
        BooleanCondition and = new BooleanConditionImpl(ConditionOperator.AND);
        and.addSubCondition(captureBinary(first, cf, r));
        String fieldName;
        while ((fieldName = r.nextKey()) != null) {
            if ("$or".equals(fieldName) || "$and".equals(fieldName)) {
                throw new BadRequestException("$or/$and must be the only key of a condition object");
            }
            and.addSubCondition(captureBinary(fieldName, cf, r));
        }
        return and;
    }

    /// Parses the value of a {@code $or}/{@code $and} key — an array of condition objects
    /// (combined with {@code op}) or a single object treated as an {@code AND} field-map — and
    /// enforces that the logical key was the only key of its enclosing object.
    private BooleanCondition parseLogicalGroup(ConditionOperator op, ConditionFactory cf, StructureReader r)
            throws SpeedyHttpException {
        BooleanCondition result;
        Kind kind = r.currentKind();
        if (kind == Kind.ARRAY) {
            result = new BooleanConditionImpl(op);
            Kind elem;
            while ((elem = r.nextElement()) != null) {
                if (elem != Kind.OBJECT) {
                    throw new BadRequestException("Invalid query ");
                }
                result.addSubCondition(parseBoolean(cf, r));
            }
        } else if (kind == Kind.OBJECT) {
            result = parseAndFieldMap(cf, r);
        } else {
            throw new BadRequestException("Invalid query");
        }
        if (r.nextKey() != null) {
            throw new BadRequestException("$or/$and must be the only key of a condition object");
        }
        return result;
    }

    private BooleanCondition parseAndFieldMap(ConditionFactory cf, StructureReader r) throws SpeedyHttpException {
        BooleanCondition and = new BooleanConditionImpl(ConditionOperator.AND);
        String fieldName;
        while ((fieldName = r.nextKey()) != null) {
            and.addSubCondition(captureBinary(fieldName, cf, r));
        }
        return and;
    }

    /// Builds a single binary condition for {@code fieldName} from the current value token: a
    /// scalar shorthand ({@code $isnull}/{@code $isnotnull} or an {@code EQ}), or an operator
    /// object ({@code {"$gt": …}}). Port of the legacy {@code captureSingleBinaryQuery}.
    private BinaryCondition captureBinary(String fieldName, ConditionFactory cf, StructureReader r)
            throws SpeedyHttpException {
        QueryField queryField = cf.createQueryField(fieldName);
        FieldMetadata metadata = queryField.getMetadataForParsing();
        Kind kind = r.currentKind();
        if (kind == Kind.OBJECT) {
            return captureOperatorCondition(queryField, metadata, cf, r);
        }
        if (kind == Kind.ARRAY) {
            throw new BadRequestException("Invalid query");
        }
        String text = r.textValue();
        if (text != null) {
            if ("$isnull".equals(text)) {
                return cf.createBiCondition(queryField, ConditionOperator.ISNULL, new Literal(new SpeedyBoolean(true)));
            }
            if ("$isnotnull".equals(text)) {
                return cf.createBiCondition(queryField, ConditionOperator.ISNOTNULL, new Literal(new SpeedyBoolean(true)));
            }
        }
        Expression expression = buildExpression(metadata, cf, r);
        return cf.createBiCondition(queryField, ConditionOperator.EQ, expression);
    }

    private BinaryCondition captureOperatorCondition(QueryField queryField, FieldMetadata metadata,
                                                     ConditionFactory cf, StructureReader r) throws SpeedyHttpException {
        String operatorSymbol = r.nextKey();
        if (operatorSymbol == null) {
            throw new BadRequestException("Invalid query");
        }
        ConditionOperator operator = ConditionOperator.fromSymbol(operatorSymbol);
        Kind valueKind = r.currentKind();
        BinaryCondition condition;
        if (operator.doesAcceptMultipleValues() && valueKind == Kind.ARRAY) {
            List<SpeedyValue> values = new LinkedList<>();
            Kind elem;
            while ((elem = r.nextElement()) != null) {
                if (elem == Kind.VALUE) {
                    values.add(r.readField(metadata));
                } else {
                    r.skipValue();
                }
            }
            condition = cf.createBiCondition(queryField, operator, new Literal(new SpeedyCollection(values)));
        } else if (operator == ConditionOperator.ISNULL || operator == ConditionOperator.ISNOTNULL) {
            if (!r.isBoolValue()) {
                throw new BadRequestException("$" + operator.name().toLowerCase() + " only accepts a boolean value");
            }
            condition = cf.createBiCondition(queryField, operator, new Literal(new SpeedyBoolean(r.boolValue())));
        } else if (valueKind == Kind.VALUE || valueKind == Kind.NULL) {
            Expression expression = buildExpression(metadata, cf, r);
            condition = cf.createBiCondition(queryField, operator, expression);
        } else {
            throw new BadRequestException("Invalid query");
        }
        // Legacy honors only the first operator of a field's operator object — drain the rest.
        while (r.nextKey() != null) {
            r.skipValue();
        }
        return condition;
    }

    /// A {@code $field}-prefixed string is a field reference ({@link Identifier}); anything else
    /// is a {@link Literal} decoded by the format. Peeking the text does not advance the cursor,
    /// so the literal branch can still {@link StructureReader#readField} the same token.
    private Expression buildExpression(FieldMetadata metadata, ConditionFactory cf, StructureReader r)
            throws SpeedyHttpException {
        String text = r.textValue();
        if (text != null && text.startsWith("$")) {
            QueryField queryField = cf.createQueryField(text.substring(1));
            cf.validateQueryFieldNotSensitive(queryField);
            return new Identifier(queryField);
        }
        return new Literal(r.readField(metadata));
    }

    private void buildSelect(SpeedyQueryImpl query, StructureReader r) throws SpeedyHttpException {
        Kind kind = r.currentKind();
        if (kind == Kind.ARRAY) {
            Kind elem;
            while ((elem = r.nextElement()) != null) {
                String value = elem == Kind.VALUE ? r.textValue() : null;
                if (value != null) {
                    applySelect(query, value);
                } else {
                    r.skipValue();
                }
            }
        } else if (kind == Kind.VALUE) {
            String value = r.textValue();
            if (value != null) {
                applySelect(query, value);
            }
        } else {
            r.skipValue();
        }
        if (query.isCountRequest() && !query.getSelect().isEmpty()) {
            throw new BadRequestException(
                    "$select cannot mix '$count' with field names. Use '$count' alone to request a count.");
        }
    }

    private void applySelect(SpeedyQueryImpl query, String value) {
        if ("$count".equals(value)) {
            query.setCountRequest(true);
        } else {
            query.addSelect(value);
        }
    }

    private void buildOrderBy(SpeedyQueryImpl query, StructureReader r) throws SpeedyHttpException {
        if (r.currentKind() != Kind.OBJECT) {
            r.skipValue();
            return;
        }
        String fieldName;
        while ((fieldName = r.nextKey()) != null) {
            String direction = r.textValue();
            if (direction != null && direction.equalsIgnoreCase("ASC")) {
                query.orderByAsc(fieldName);
            } else if (direction != null && direction.equalsIgnoreCase("DESC")) {
                query.orderByDesc(fieldName);
            } else {
                throw new BadRequestException("order by should be field name: asc|desc");
            }
        }
    }

    private void buildPaging(SpeedyQueryImpl query, StructureReader r) throws SpeedyHttpException {
        if (r.currentKind() != Kind.OBJECT) {
            r.skipValue();
            return;
        }
        String key;
        while ((key = r.nextKey()) != null) {
            switch (key) {
                case "$index" -> {
                    if (r.currentKind() != Kind.NULL) {
                        query.addPageNo(r.intValue());
                    }
                }
                case "$size" -> {
                    if (r.currentKind() != Kind.NULL) {
                        int pageSize = r.intValue();
                        if (pageSize > query.getMaxPageSize()) {
                            throw new BadRequestException(
                                    "Requested page size " + pageSize + " exceeds maximum allowed page size " + query.getMaxPageSize());
                        }
                        query.addPageSize(pageSize);
                    }
                }
                default -> r.skipValue();
            }
        }
    }

    private void buildExpand(SpeedyQueryImpl query, StructureReader r) throws SpeedyHttpException {
        if (r.currentKind() != Kind.ARRAY) {
            r.skipValue();
            return;
        }
        Kind elem;
        while ((elem = r.nextElement()) != null) {
            String value = elem == Kind.VALUE ? r.textValue() : null;
            if (value != null) {
                validateExpansionPath(value);
                query.addExpand(value);
            } else {
                r.skipValue();
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
}
