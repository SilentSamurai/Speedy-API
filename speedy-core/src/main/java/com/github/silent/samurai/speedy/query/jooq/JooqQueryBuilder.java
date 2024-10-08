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
import com.github.silent.samurai.speedy.utils.SpeedyValueFactory;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class JooqQueryBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(JooqQueryBuilder.class);

    private final SpeedyQuery speedyQuery;
    private final EntityMetadata entityMetadata;
    private final DSLContext dslContext;
    private final List<FieldMetadata> joins = new LinkedList<>();
    private final SelectJoinStep<org.jooq.Record> query;

    public JooqQueryBuilder(SpeedyQuery speedyQuery, DSLContext dslContext) {
        this.speedyQuery = speedyQuery;
        this.entityMetadata = speedyQuery.getFrom();
        this.dslContext = dslContext;
        this.query = this.dslContext.select()
                .from(JooqUtil.getTable(speedyQuery.getFrom()));
    }

    Field<Object> getPath(BinaryCondition bCondition) {
        QueryField queryField = bCondition.getField();
        if (queryField.isAssociated()) {
            FieldMetadata associatedMetadata = queryField.getAssociatedFieldMetadata();
            joins.add(queryField.getFieldMetadata());

            return JooqUtil.getColumn(associatedMetadata);
        } else {
            FieldMetadata fieldMetadata = queryField.getFieldMetadata();;
            return JooqUtil.getColumn(fieldMetadata);
        }
    }

    Object getRawValue(BinaryCondition bCondition, SpeedyValue speedyValue) throws SpeedyHttpException {
        QueryField queryField = bCondition.getField();
        if (queryField.isAssociated()) {
            return SpeedyValueFactory.toJavaType(queryField.getAssociatedFieldMetadata(), speedyValue);
        }
        return SpeedyValueFactory.toJavaType(bCondition.getField().getFieldMetadata(), speedyValue);
    }

    org.jooq.Condition captureBooleanPredicate(BooleanCondition condition) throws Exception {
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

    org.jooq.Condition equalPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        SpeedyValue speedyValue = bCondition.getSpeedyValue();
        if (!speedyValue.isValue()) {
            throw new BadRequestException("OBJECT & COLLECTION Operation not supported");
        }
        if (speedyValue.isNull()) {
            Field<?> field = getPath(bCondition);
            return field.isNull();
        } else {
            Field<Object> path = getPath(bCondition);
            Object rawValue = getRawValue(bCondition, speedyValue);
            return path.equal(rawValue);
        }
    }

    org.jooq.Condition notEqualPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        SpeedyValue speedyValue = bCondition.getSpeedyValue();
        if (!speedyValue.isValue()) {
            throw new BadRequestException("OBJECT & COLLECTION Operation not supported");
        }
        if (speedyValue.isNull()) {
            Field<Object> field = getPath(bCondition);
            return field.isNotNull();
        } else {
            Field<Object> path = getPath(bCondition);
            Object rawValue = getRawValue(bCondition, speedyValue);
            return path.notEqual(rawValue);
        }
    }

    org.jooq.Condition lessThanPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        SpeedyValue speedyValue = bCondition.getSpeedyValue();
        if (!speedyValue.isValue()) {
            throw new BadRequestException("OBJECT & COLLECTION Operation not supported");
        }
        Field<Object> path = getPath(bCondition);
        Object rawValue = getRawValue(bCondition, speedyValue);
        return path.lessThan(rawValue);
    }

    org.jooq.Condition greaterThanPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        SpeedyValue speedyValue = bCondition.getSpeedyValue();
        if (!speedyValue.isValue()) {
            throw new BadRequestException("OBJECT & COLLECTION Operation not supported");
        }
        Field<Object> path = getPath(bCondition);
        Object rawValue = getRawValue(bCondition, speedyValue);
        return path.greaterThan(rawValue);
    }

    org.jooq.Condition lessThanOrEqualToPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        SpeedyValue speedyValue = bCondition.getSpeedyValue();
        if (!speedyValue.isValue()) {
            throw new BadRequestException("OBJECT & COLLECTION Operation not supported");
        }
        Field<Object> path = getPath(bCondition);
        Object rawValue = getRawValue(bCondition, speedyValue);
        return path.lessOrEqual(rawValue);
    }

    org.jooq.Condition greaterThanOrEqualToPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        SpeedyValue speedyValue = bCondition.getSpeedyValue();
        if (!speedyValue.isValue()) {
            throw new BadRequestException("OBJECT & COLLECTION Operation not supported");
        }
        Field<Object> path = getPath(bCondition);
        Object rawValue = getRawValue(bCondition, speedyValue);
        return path.greaterOrEqual(rawValue);
    }

    org.jooq.Condition inPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        SpeedyValue speedyValue = bCondition.getSpeedyValue();
        if (speedyValue.isCollection()) {
            FieldMetadata fieldMetadata = bCondition.getField().getFieldMetadata();
            if (!fieldMetadata.isAssociation()) {
                Field<Object> path = getPath(bCondition);
                Collection<SpeedyValue> collection = speedyValue.asCollection();
                Collection<Object> objects = new ArrayList<>(collection.size());
                for (SpeedyValue sv : collection) {
                    Object rawValue = getRawValue(bCondition, sv);
                    objects.add(rawValue);
                }
                return path.in(objects);
            }
            throw new BadRequestException("COLLECTION of Association Operation not supported");
        }
        if (!speedyValue.isValue()) {
            throw new BadRequestException("OBJECT Operation not supported");
        }
        Field<Object> path = getPath(bCondition);
        Object rawValue = getRawValue(bCondition, speedyValue);
        return path.in(rawValue);
    }

    org.jooq.Condition notInPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        SpeedyValue speedyValue = bCondition.getSpeedyValue();
        if (speedyValue.isCollection()) {
            FieldMetadata fieldMetadata = bCondition.getField().getFieldMetadata();
            if (!fieldMetadata.isAssociation()) {
                Field<Object> path = getPath(bCondition);
                Collection<SpeedyValue> collection = speedyValue.asCollection();
                Collection<Object> objects = new ArrayList<>(collection.size());
                for (SpeedyValue sv : collection) {
                    Object rawValue = getRawValue(bCondition, sv);
                    objects.add(rawValue);
                }
                return path.notIn(objects);
            }
            throw new BadRequestException("COLLECTION of Association Operation not supported");
        }
        if (!speedyValue.isValue()) {
            throw new BadRequestException("OBJECT Operation not supported");
        }
        Field<Object> path = getPath(bCondition);
        Object rawValue = getRawValue(bCondition, speedyValue);
        return path.notIn(List.of(rawValue));
    }

    org.jooq.Condition conditionToPredicate(Condition condition) throws Exception {
        ConditionOperator operator = condition.getOperator();
        if (operator == ConditionOperator.AND || operator == ConditionOperator.OR) {
            return captureBooleanPredicate((BooleanCondition) condition);
        }

        BinaryCondition bCondition = (BinaryCondition) condition;

        switch (condition.getOperator()) {
            case EQ:
                return equalPredicate(bCondition);
            case NEQ:
                return notEqualPredicate(bCondition);
            case LT:
                return lessThanPredicate(bCondition);
            case GT:
                return greaterThanPredicate(bCondition);
            case LTE:
                return lessThanOrEqualToPredicate(bCondition);
            case GTE:
                return greaterThanOrEqualToPredicate(bCondition);
            case IN:
                return inPredicate(bCondition);
            case NOT_IN:
                return notInPredicate(bCondition);
            default:
                throw new BadRequestException("Unknown Operator");
        }
    }

    void captureOrderBy() {
        for (OrderBy orderBy : speedyQuery.getOrderByList()) {
            FieldMetadata fieldMetadata = orderBy.getFieldMetadata();
            Field<Object> field = JooqUtil.getColumn(fieldMetadata);
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
        int offset = (pageNumber) * pageSize;
        query.limit(pageSize)
                .offset(offset);
    }

    private void joins() {
        for (FieldMetadata join : joins) {
            // the foreign key table to join
            Table<?> table = JooqUtil.getTable(join.getAssociationMetadata());
            // foreign key field
            Field<?> fromField = JooqUtil.getColumn(join);
            // primary key field, from foreign key
            Field joinField = JooqUtil.getColumn(join.getAssociatedFieldMetadata());
            query.join(table)
                    .on(fromField.eq(joinField));
        }
    }

    public Result<Record> executeQuery() throws Exception {
        if (Objects.nonNull(speedyQuery.getWhere())) {
            org.jooq.Condition whereCondition = conditionToPredicate(this.speedyQuery.getWhere());
            query.where(whereCondition);
        }
        captureOrderBy();
        addPageInfo();
        joins();

        LOGGER.info("query sql: {} ", query.toString());
        return query.fetch();
    }


}
