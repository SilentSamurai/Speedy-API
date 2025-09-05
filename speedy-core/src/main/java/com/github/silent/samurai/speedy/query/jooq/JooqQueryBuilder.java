package com.github.silent.samurai.speedy.query.jooq;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.enums.OrderByOperator;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.*;
import com.github.silent.samurai.speedy.interfaces.query.Condition;
import com.github.silent.samurai.speedy.interfaces.query.Converter;
import com.github.silent.samurai.speedy.models.conditions.EqCondition;
import com.github.silent.samurai.speedy.models.conditions.InCondition;
import com.github.silent.samurai.speedy.models.conditions.MatchingCondition;
import com.github.silent.samurai.speedy.models.conditions.NotInCondition;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;


public class JooqQueryBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(JooqQueryBuilder.class);

    final SpeedyQuery speedyQuery;
    final EntityMetadata entityMetadata;
    final DSLContext dslContext;
    final Map<String, FieldMetadata> joins = new HashMap<>();
    final Map<String, String> joinAlias = new HashMap<>();
    final SQLDialect dialect;
    SelectJoinStep<? extends Record> query;
    private final Converter converter;

    public JooqQueryBuilder(SpeedyQuery speedyQuery, DSLContext dslContext, Converter converter) {
        this.speedyQuery = speedyQuery;
        this.entityMetadata = speedyQuery.getFrom();
        this.dslContext = dslContext;
        this.dialect = dslContext.dialect();
        this.converter = converter;
    }

    Object toJooqType(BinaryCondition bCondition, SpeedyValue speedyValue) throws SpeedyHttpException {
        QueryField queryField = bCondition.getField();
        if (queryField.isAssociated()) {
            return converter.toColumnType(
                    speedyValue,
                    queryField.getAssociatedFieldMetadata()
            );
        }
        return converter.toColumnType(
                speedyValue,
                bCondition.getField().getFieldMetadata()
        );
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

    org.jooq.Condition matchPredicate(MatchingCondition bCondition) throws SpeedyHttpException {
        SpeedyValue speedyValue = bCondition.getExpression().value();
        if (!speedyValue.isText()) {
            throw new BadRequestException("only text values are supported for $matches.");
        }
        Field<Object> path = getPath(bCondition.getField());
        String rawValue = speedyValue.asText()
                .replaceAll("%", "\\\\%")
                .replaceAll("\\*", "%");
        return path.like(DSL.value(rawValue)).escape('\\');
    }

    org.jooq.Condition equalPredicate(EqCondition bCondition) throws SpeedyHttpException {
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
            throw new BadRequestException("Not Reachable");
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
            throw new BadRequestException("Not Reachable");
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
            throw new BadRequestException("Not Reachable");
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
            throw new BadRequestException("Not Reachable");
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
            throw new BadRequestException("Not Reachable");
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
            throw new BadRequestException("Not Reachable");
        }
    }

    org.jooq.Condition inPredicate(InCondition bCondition) throws SpeedyHttpException {
        SpeedyValue speedyValue = bCondition.getExpression().value();
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

    org.jooq.Condition notInPredicate(NotInCondition bCondition) throws SpeedyHttpException {
        SpeedyValue speedyValue = bCondition.getExpression().value();
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

    org.jooq.Condition conditionToPredicate(Condition condition) throws SpeedyHttpException {
        ConditionOperator operator = condition.getOperator();
        if (operator == ConditionOperator.AND || operator == ConditionOperator.OR) {
            return captureBooleanPredicate((BooleanCondition) condition);
        }

        BinaryCondition bCondition = (BinaryCondition) condition;

        return switch (condition.getOperator()) {
            case EQ:
                yield equalPredicate((EqCondition) bCondition);
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
                yield inPredicate((InCondition) bCondition);
            case NOT_IN:
                yield notInPredicate((NotInCondition) bCondition);
            case PATTERN_MATCHING:
                yield matchPredicate((MatchingCondition) bCondition);
            case AND:
            case OR:
                throw new BadRequestException("Unknown Operator");
        };
    }

    void captureOrderBy() {
        for (OrderBy orderBy : speedyQuery.getOrderByList()) {
            FieldMetadata fieldMetadata = orderBy.getFieldMetadata();
            Field<Object> field = JooqUtil.getColumn(fieldMetadata, dialect);
            OrderByOperator operator = orderBy.getOperator();
            if (operator == OrderByOperator.ASC) {
                query.orderBy(field.asc());
            }
            if (operator == OrderByOperator.DESC) {
                query.orderBy(field.desc());
            }
        }
    }

    private void addPageInfo() throws BadRequestException {
        PageInfo pageInfo = speedyQuery.getPageInfo();
        int pageSize = pageInfo.getPageSize();
        int pageNumber = pageInfo.getPageNo();
        long offset = (long) (pageNumber) * pageSize;
        query.limit(offset, pageSize);
    }

    // foreign key table and fkfield
    String getJoinKey(EntityMetadata fkEntityMetadata, FieldMetadata fieldMetadata) {
        return String.format("%s.%s", fkEntityMetadata.getDbTableName(), fieldMetadata.getDbColumnName());
    }

    String getTableAlias(EntityMetadata fkEntityMetadata) {
        return String.format("%s_%s", fkEntityMetadata.getName(), joinAlias.size() + 1);
    }

    Field<Object> getPath(QueryField queryField) {
        if (queryField.isAssociated()) {
            FieldMetadata fkMetadata = queryField.getAssociatedFieldMetadata();
            String key = getJoinKey(fkMetadata.getEntityMetadata(), queryField.getFieldMetadata());
            joins.put(key, queryField.getFieldMetadata());
            if (!joinAlias.containsKey(key)) {
                String alias = getTableAlias(fkMetadata.getEntityMetadata());
                joinAlias.put(key, alias);
            }
            String alias = joinAlias.get(key);
            return JooqUtil.getColumnWithTableAlias(alias, fkMetadata, dialect);
        } else {
            FieldMetadata fieldMetadata = queryField.getFieldMetadata();
            return JooqUtil.getColumn(fieldMetadata, dialect);
        }
    }

    private void joins() {
        for (FieldMetadata join : joins.values()) {
            // the foreign key table to join
            Table<?> table = JooqUtil.getTable(join.getAssociationMetadata(), dialect);
            // foreign key field
            Field<?> fromField = JooqUtil.getColumn(join, dialect);
            // primary key field, from foreign table
            // Field joinField = JooqUtil.getColumn(join.getAssociatedFieldMetadata(), dialect);
            String joinKey = getJoinKey(join.getAssociationMetadata(), join);
            String alias = joinAlias.get(joinKey);

            Field joinField = JooqUtil.getColumnWithTableAlias(alias, join.getAssociatedFieldMetadata(), dialect);
            query.join(table.as(alias)).on(fromField.eq(joinField));
        }
    }

    void prepareQuery() throws SpeedyHttpException {
        this.query = this.dslContext.select()
                .from(JooqUtil.getTable(speedyQuery.getFrom(), dialect));
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
            SelectConditionStep<? extends Record> where = query.where(whereCondition);
            joins();
        }
        LOGGER.debug("SQL Count Query: {} ", query.toString());
        return query.fetchOne(0, BigInteger.class);
    }

}
