package com.github.silent.samurai.speedy.jooq.impl.query;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.enums.OrderByOperator;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.metadata.AssociationColumn;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.*;
import com.github.silent.samurai.speedy.interfaces.query.Condition;
import com.github.silent.samurai.speedy.jooq.impl.Dialects;
import com.github.silent.samurai.speedy.jooq.impl.conversion.TypeConverter;
import com.github.silent.samurai.speedy.jooq.impl.dialect.DefaultDialect;

import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;


/// Translates SpeedyQuery condition tree into jOOQ Condition predicates.
/// Handles JOINs for associations, ordering, pagination, and field selection.
public class JooqQueryBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(JooqQueryBuilder.class);

    final SpeedyQuery speedyQuery;
    final EntityMetadata entityMetadata;
    final DSLContext dslContext;
    final Map<String, FieldMetadata> joins = new HashMap<>();
    final Map<String, String> joinAlias = new HashMap<>();
    final SQLDialect dialect;
    private final TypeConverter converter;
    SelectJoinStep<? extends Record> query;

    public JooqQueryBuilder(SpeedyQuery speedyQuery, DSLContext dslContext, TypeConverter converter) {
        this.speedyQuery = speedyQuery;
        this.entityMetadata = speedyQuery.getFrom();
        this.dslContext = dslContext;
        this.dialect = dslContext.dialect();
        this.converter = converter;
    }

    Object toJooqType(BinaryCondition bCondition, SpeedyValue speedyValue) throws SpeedyHttpException {
        // The converter is dialect-aware, so the encoded value is already dialect-correct.
        return converter.toColumnType(speedyValue, conversionField(bCondition.getField()));
    }

    /// The metadata describing a query field's value type. When the query path navigates into an
    /// association the value is typed by the associated (target) field; otherwise by the field itself.
    /// This keys off the {@code QueryField}'s own association flag (set during query construction),
    /// which is deliberately distinct from {@link FieldMetadata#isAssociation()} — see {@code NormalField}
    /// (always non-associated, even when wrapping an FK column) vs {@code AssociatedField}.
    private static FieldMetadata conversionField(QueryField queryField) {
        return queryField.isAssociated()
                ? queryField.getAssociatedFieldMetadata()
                : queryField.getFieldMetadata();
    }

    org.jooq.Condition captureBooleanPredicate(BooleanCondition condition) throws SpeedyHttpException {
        List<org.jooq.Condition> predicates = new ArrayList<>();
        for (Condition subCondition : condition.getConditions()) {
            org.jooq.Condition jqCondition = conditionToPredicate(subCondition);
            predicates.add(jqCondition);
        }
        if (condition.getOperator() == ConditionOperator.OR) {
            return predicates.stream()
                    .reduce(DSL.noCondition(), org.jooq.Condition::or);
        }
        return predicates.stream()
                .reduce(DSL.noCondition(), org.jooq.Condition::and);
    }

    org.jooq.Condition matchPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        SpeedyValue speedyValue = bCondition.getLiteral().value();
        if (!speedyValue.isText()) {
            throw new BadRequestException("only text values are supported for $matches.");
        }
        Field<Object> path = getPath(bCondition.getField());
        String rawValue = speedyValue.asText()
                .replaceAll("%", "\\\\%")
                .replaceAll("_", "\\\\_")
                .replaceAll("\\*", "%");
        return path.like(DSL.value(rawValue)).escape('\\');
    }

    org.jooq.Condition equalPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        Expression expression = bCondition.getExpression();
        if (expression instanceof Literal l) {
            SpeedyValue speedyValue = l.value();
            if (!speedyValue.isValue()) {
                throw new BadRequestException("OBJECT & COLLECTION Operation not supported");
            }
            if (speedyValue.isNull()) {
                Field<?> field = getPath(bCondition.getField());
                return field.isNull();
            } else {
                Field<Object> path = getPath(bCondition.getField());
                Object rawValue = toJooqType(bCondition, speedyValue);
                return path.equal(DSL.value(rawValue));
            }
        } else if (expression instanceof Identifier identifier) {
            Field<Object> path = getPath(bCondition.getField());
            Field<Object> value = getPath(identifier.field());
            return path.equal(value);
        } else {
            throw new BadRequestException("Unresolved variable reference in query condition; variable references must be resolved before SQL generation");
        }
    }

    org.jooq.Condition notEqualPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        Expression expression = bCondition.getExpression();
        if (expression instanceof Literal l) {
            SpeedyValue speedyValue = l.value();
            if (!speedyValue.isValue()) {
                throw new BadRequestException("OBJECT & COLLECTION Operation not supported");
            }
            if (speedyValue.isNull()) {
                Field<Object> field = getPath(bCondition.getField());
                return field.isNotNull();
            } else {
                Field<Object> path = getPath(bCondition.getField());
                Object rawValue = toJooqType(bCondition, speedyValue);
                return path.notEqual(DSL.value(rawValue));
            }
        } else if (expression instanceof Identifier identifier) {
            Field<Object> path = getPath(bCondition.getField());
            Field<Object> value = getPath(identifier.field());
            return path.notEqual(value);
        } else {
            throw new BadRequestException("Unresolved variable reference in query condition; variable references must be resolved before SQL generation");
        }
    }

    org.jooq.Condition lessThanPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        Expression expression = bCondition.getExpression();
        if (expression instanceof Literal l) {
            SpeedyValue speedyValue = l.value();
            if (!speedyValue.isValue()) {
                throw new BadRequestException("OBJECT & COLLECTION Operation not supported");
            }
            Field<Object> path = getPath(bCondition.getField());
            Object rawValue = toJooqType(bCondition, speedyValue);
            return path.lessThan(DSL.value(rawValue));
        } else if (expression instanceof Identifier identifier) {
            Field<Object> path = getPath(bCondition.getField());
            Field<Object> value = getPath(identifier.field());
            return path.lessThan(value);
        } else {
            throw new BadRequestException("Unresolved variable reference in query condition; variable references must be resolved before SQL generation");
        }
    }

    org.jooq.Condition greaterThanPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        Expression expression = bCondition.getExpression();
        if (expression instanceof Literal l) {
            SpeedyValue speedyValue = l.value();
            if (!speedyValue.isValue()) {
                throw new BadRequestException("OBJECT & COLLECTION Operation not supported");
            }
            Field<Object> path = getPath(bCondition.getField());
            Object rawValue = toJooqType(bCondition, speedyValue);
            return path.greaterThan(DSL.value(rawValue));
        } else if (expression instanceof Identifier identifier) {
            Field<Object> path = getPath(bCondition.getField());
            Field<Object> value = getPath(identifier.field());
            return path.greaterThan(value);
        } else {
            throw new BadRequestException("Unresolved variable reference in query condition; variable references must be resolved before SQL generation");
        }
    }

    org.jooq.Condition lessThanOrEqualToPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        Expression expression = bCondition.getExpression();
        if (expression instanceof Literal l) {
            SpeedyValue speedyValue = l.value();
            if (!speedyValue.isValue()) {
                throw new BadRequestException("OBJECT & COLLECTION Operation not supported");
            }
            Field<Object> path = getPath(bCondition.getField());
            Object rawValue = toJooqType(bCondition, speedyValue);
            return path.lessOrEqual(DSL.value(rawValue));
        } else if (expression instanceof Identifier identifier) {
            Field<Object> path = getPath(bCondition.getField());
            Field<Object> value = getPath(identifier.field());
            return path.lessOrEqual(value);
        } else {
            throw new BadRequestException("Unresolved variable reference in query condition; variable references must be resolved before SQL generation");
        }
    }

    org.jooq.Condition greaterThanOrEqualToPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        Expression expression = bCondition.getExpression();
        if (expression instanceof Literal l) {
            SpeedyValue speedyValue = l.value();
            if (!speedyValue.isValue()) {
                throw new BadRequestException("OBJECT & COLLECTION Operation not supported");
            }
            Field<Object> path = getPath(bCondition.getField());
            Object rawValue = toJooqType(bCondition, speedyValue);
            return path.greaterOrEqual(DSL.value(rawValue));
        } else if (expression instanceof Identifier identifier) {
            Field<Object> path = getPath(bCondition.getField());
            Field<Object> value = getPath(identifier.field());
            return path.greaterOrEqual(value);
        } else {
            throw new BadRequestException("Unresolved variable reference in query condition; variable references must be resolved before SQL generation");
        }
    }

    org.jooq.Condition inPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        SpeedyValue speedyValue = bCondition.getLiteral().value();
        if (speedyValue.isCollection()) {
            FieldMetadata fieldMetadata = bCondition.getField().getFieldMetadata();
            if (!fieldMetadata.isAssociation()) {
                Field<Object> path = getPath(bCondition.getField());
                Collection<SpeedyValue> collection = speedyValue.asCollection();
                Collection<Param<Object>> objects = new ArrayList<>(collection.size());
                for (SpeedyValue sv : collection) {
                    Object rawValue = toJooqType(bCondition, sv);
                    objects.add(DSL.value(rawValue));
                }
                return path.in(objects);
            }
            throw new BadRequestException("COLLECTION of Association Operation not supported");
        }
        if (!speedyValue.isValue()) {
            throw new BadRequestException("OBJECT Operation not supported");
        }
        Field<Object> path = getPath(bCondition.getField());
        Object rawValue = toJooqType(bCondition, speedyValue);
        return path.in(DSL.value(rawValue));
    }

    org.jooq.Condition notInPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        SpeedyValue speedyValue = bCondition.getLiteral().value();
        if (speedyValue.isCollection()) {
            FieldMetadata fieldMetadata = bCondition.getField().getFieldMetadata();
            if (!fieldMetadata.isAssociation()) {
                Field<Object> path = getPath(bCondition.getField());
                Collection<SpeedyValue> collection = speedyValue.asCollection();
                Collection<Param<Object>> objects = new ArrayList<>(collection.size());
                for (SpeedyValue sv : collection) {
                    Object rawValue = toJooqType(bCondition, sv);
                    objects.add(DSL.value(rawValue));
                }
                return path.notIn(objects);
            }
            throw new BadRequestException("COLLECTION of Association Operation not supported");
        }
        if (!speedyValue.isValue()) {
            throw new BadRequestException("OBJECT Operation not supported");
        }
        Field<Object> path = getPath(bCondition.getField());
        Object rawValue = toJooqType(bCondition, speedyValue);
        return path.notIn(List.of(DSL.value(rawValue)));
    }

    /**
     * Translates a {@code $between} condition to a JOOQ {@code Field.between(low, high)} predicate.
     */
    org.jooq.Condition betweenPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        SpeedyValue speedyValue = bCondition.getLiteral().value();
        SpeedyValue[] arr = speedyValue.asCollection().toArray(new SpeedyValue[0]);
        Field<Object> path = getPath(bCondition.getField());
        Object low = toJooqType(bCondition, arr[0]);
        Object high = toJooqType(bCondition, arr[1]);
        return path.between(DSL.value(low), DSL.value(high));
    }

    /**
     * Translates an {@code $isnull} condition to a JOOQ {@code Field.isNull()} predicate. A
     * multi-column foreign key is null only when every one of its columns is.
     */
    org.jooq.Condition isNullPredicate(BinaryCondition bCondition) {
        org.jooq.Condition predicate = DSL.noCondition();
        for (Field<Object> path : getPaths(bCondition.getField())) {
            predicate = predicate.and(path.isNull());
        }
        return predicate;
    }

    /**
     * Translates an {@code $isnotnull} condition to a JOOQ {@code Field.isNotNull()} predicate. A
     * multi-column foreign key is set as soon as any of its columns is.
     */
    org.jooq.Condition isNotNullPredicate(BinaryCondition bCondition) {
        org.jooq.Condition predicate = DSL.noCondition();
        for (Field<Object> path : getPaths(bCondition.getField())) {
            predicate = predicate.or(path.isNotNull());
        }
        return predicate;
    }

    org.jooq.Condition conditionToPredicate(Condition condition) throws SpeedyHttpException {
        ConditionOperator operator = condition.getOperator();
        if (operator == ConditionOperator.AND || operator == ConditionOperator.OR) {
            return captureBooleanPredicate((BooleanCondition) condition);
        }

        BinaryCondition bCondition = (BinaryCondition) condition;
        rejectScalarComparisonOnCompositeForeignKey(bCondition);

        return switch (condition.getOperator()) {
            case EQ:
                yield equalPredicate(bCondition);
            case NEQ:
                yield notEqualPredicate(bCondition);
            case LT:
                yield lessThanPredicate(bCondition);
            case GT:
                yield greaterThanPredicate(bCondition);
            case LTE:
                yield lessThanOrEqualToPredicate(bCondition);
            case GTE:
                yield greaterThanOrEqualToPredicate(bCondition);
            case IN:
                yield inPredicate(bCondition);
            case NOT_IN:
                yield notInPredicate(bCondition);
            case PATTERN_MATCHING:
                yield matchPredicate(bCondition);
            case BETWEEN:
                yield betweenPredicate(bCondition);
            case ISNULL:
                yield isNullPredicate(bCondition);
            case ISNOTNULL:
                yield isNotNullPredicate(bCondition);
            case AND:
            case OR:
                throw new BadRequestException("Unknown Operator");
        };
    }

    void captureOrderBy() {
        for (OrderBy orderBy : speedyQuery.getOrderByList()) {
            OrderByOperator operator = orderBy.getOperator();
            // Ordering by a multi-column foreign key orders by its columns in key order, the same
            // lexicographic order the target's own composite key sorts in.
            for (Field<Object> field : getPaths(orderBy.getField())) {
                if (operator == OrderByOperator.ASC) {
                    query.orderBy(field.asc());
                }
                if (operator == OrderByOperator.DESC) {
                    query.orderBy(field.desc());
                }
            }
        }
    }

    /// Rejects comparing a multi-column foreign key against a single value. Such a field has no one
    /// column to compare — only a navigated path ({@code order.productId}) or a null check addresses
    /// it meaningfully.
    private void rejectScalarComparisonOnCompositeForeignKey(BinaryCondition bCondition) throws BadRequestException {
        ConditionOperator operator = bCondition.getOperator();
        if (operator == ConditionOperator.ISNULL || operator == ConditionOperator.ISNOTNULL) {
            return;
        }
        QueryField queryField = bCondition.getField();
        if (queryField.isAssociated() || !queryField.getFieldMetadata().isCompositeAssociation()) {
            return;
        }
        throw new BadRequestException(String.format(
                "'%s' is a foreign key spanning %d columns and cannot be compared to a single value — " +
                        "reference one of %s's key fields instead (e.g. '%s.%s')",
                queryField.getFieldMetadata().getOutputPropertyName(),
                queryField.getFieldMetadata().getAssociationColumns().size(),
                queryField.getFieldMetadata().getAssociationMetadata().getName(),
                queryField.getFieldMetadata().getOutputPropertyName(),
                queryField.getFieldMetadata().getAssociatedFieldMetadata().getOutputPropertyName()));
    }

    private void addPageInfo() throws BadRequestException {
        PageInfo pageInfo = speedyQuery.getPageInfo();
        int pageSize = pageInfo.getPageSize();
        int pageNumber = pageInfo.getPageNo();
        long offset = (long) (pageNumber) * pageSize;
        query.limit(offset, pageSize);
    }

    // foreign key table and fkfield(s) — all of them, so two associations to the same table that
    // happen to share their first column still get their own join.
    String getJoinKey(EntityMetadata fkEntityMetadata, FieldMetadata fieldMetadata) {
        String columns = fieldMetadata.getAssociationColumns().stream()
                .map(AssociationColumn::localDbColumnName)
                .collect(Collectors.joining(","));
        return String.format("%s.%s", fkEntityMetadata.getDbTableName(),
                columns.isEmpty() ? fieldMetadata.getDbColumnName() : columns);
    }

    String getTableAlias(EntityMetadata fkEntityMetadata) {
        return String.format("%s_%s", fkEntityMetadata.getName(), joinAlias.size() + 1);
    }

    /// The column in the form a comparison and an ORDER BY can rely on. Only the dialect knows
    /// whether the stored form is guaranteed — see {@link DefaultDialect#comparisonField}. The
    /// selected columns are built separately and stay untouched, so what a query returns is still the
    /// raw stored value.
    private Field<Object> comparable(Field<Object> column, FieldMetadata fieldMetadata) {
        return Dialects.forJooq(dialect).comparisonField(column, fieldMetadata.getColumnType());
    }

    Field<Object> getPath(QueryField queryField) {
        return getPaths(queryField).get(0);
    }

    /// Every column {@code queryField} addresses: one for a navigated association path or a plain
    /// column, several when the field is a foreign key spanning the columns of a composite key.
    List<Field<Object>> getPaths(QueryField queryField) {
        if (queryField.isAssociated()) {
            FieldMetadata fkMetadata = queryField.getAssociatedFieldMetadata();
            String key = getJoinKey(fkMetadata.getEntityMetadata(), queryField.getFieldMetadata());
            joins.put(key, queryField.getFieldMetadata());
            if (!joinAlias.containsKey(key)) {
                String alias = getTableAlias(fkMetadata.getEntityMetadata());
                joinAlias.put(key, alias);
            }
            String alias = joinAlias.get(key);
            return List.of(comparable(JooqUtil.getColumnWithTableAlias(alias, fkMetadata, dialect), fkMetadata));
        } else {
            FieldMetadata fieldMetadata = queryField.getFieldMetadata();
            return JooqUtil.getColumns(fieldMetadata, dialect).stream()
                    .map(column -> comparable(column, fieldMetadata))
                    .toList();
        }
    }

    private void joins() {
        for (FieldMetadata join : joins.values()) {
            // the foreign key table to join
            Table<?> table = JooqUtil.getTable(join.getAssociationMetadata(), dialect);
            // foreign key field(s)
            List<Field<Object>> fromFields = JooqUtil.getColumns(join, dialect);
            String joinKey = getJoinKey(join.getAssociationMetadata(), join);
            String alias = joinAlias.get(joinKey);

            // A multi-column foreign key matches on every column of the target's key at once.
            List<AssociationColumn> associationColumns = join.getAssociationColumns();
            org.jooq.Condition on = DSL.noCondition();
            for (int i = 0; i < fromFields.size(); i++) {
                // primary key field, from foreign table
                Field<Object> joinField = JooqUtil.getColumnWithTableAlias(
                        alias, associationColumns.get(i).targetKeyField(), dialect);
                on = on.and(fromFields.get(i).eq(joinField));
            }
            // LEFT (outer) join: filtering on an association path must not silently drop source rows
            // whose FK is null — they are kept and excluded only if the predicate itself fails.
            query.leftJoin(table.as(alias)).on(on);
        }
    }

    void prepareQuery() throws SpeedyHttpException {
        Set<String> selectFields = speedyQuery.getSelect();
        if (selectFields != null && !selectFields.isEmpty()) {
            boolean hasValidUserField = false;
            for (String fieldName : selectFields) {
                try {
                    FieldMetadata field = entityMetadata.getField(fieldName);
                    if (field.getDbColumnName() != null && !(field.isAssociation() && field.isCollection())) {
                        hasValidUserField = true;
                        break;
                    }
                } catch (NotFoundException ignored) {
                }
            }
            if (!hasValidUserField) {
                this.query = this.dslContext.select(JooqUtil.getAllColumns(speedyQuery.getFrom(), dialect))
                        .from(JooqUtil.getTable(speedyQuery.getFrom(), dialect));
            } else {
                Set<FieldMetadata> addedFields = new LinkedHashSet<>();
                for (KeyFieldMetadata keyField : entityMetadata.getKeyFields()) {
                    if (keyField.getDbColumnName() != null) {
                        addedFields.add(keyField);
                    }
                }
                for (String fieldName : selectFields) {
                    try {
                        FieldMetadata field = entityMetadata.getField(fieldName);
                        if (field.getDbColumnName() == null) continue;
                        if (field.isAssociation() && field.isCollection()) continue;
                        addedFields.add(field);
                    } catch (NotFoundException ignored) {
                    }
                }
                List<Field<?>> fields = new ArrayList<>(addedFields.size());
                for (FieldMetadata fm : addedFields) {
                    // A multi-column foreign key needs every one of its columns selected, or the
                    // association resolves to a partial key.
                    fields.addAll(JooqUtil.getColumns(fm, dialect));
                }
                this.query = this.dslContext.select(fields)
                        .from(JooqUtil.getTable(speedyQuery.getFrom(), dialect));
            }
        } else {
            // No $select: every column, still typed from the metamodel rather than left to jOOQ's
            // JDBC-metadata guess (see JooqUtil.getAllColumns).
            this.query = this.dslContext.select(JooqUtil.getAllColumns(speedyQuery.getFrom(), dialect))
                    .from(JooqUtil.getTable(speedyQuery.getFrom(), dialect));
        }
        if (Objects.nonNull(speedyQuery.getWhere())) {
            var predicates = conditionToPredicate(this.speedyQuery.getWhere());
            query.where(predicates);
        }
        captureOrderBy();
        addPageInfo();
        joins();
        LOGGER.debug("SQL Query: {} ", query.toString());
    }

    public Result<? extends Record> executeQuery() throws SpeedyHttpException {
        prepareQuery();
        return query.fetch();
    }


    public BigInteger executeCountQuery() throws SpeedyHttpException {
        this.query = this.dslContext.select(DSL.count())
                .from(JooqUtil.getTable(speedyQuery.getFrom(), dialect));
        if (Objects.nonNull(speedyQuery.getWhere())) {
            org.jooq.Condition whereCondition = conditionToPredicate(this.speedyQuery.getWhere());
            query.where(whereCondition);
            joins();
        }
        LOGGER.debug("SQL Count Query: {} ", query.toString());
        return query.fetchOne(0, BigInteger.class);
    }

}
