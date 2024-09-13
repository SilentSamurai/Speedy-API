package com.github.silent.samurai.speedy.file.impl.query;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.enums.OrderByOperator;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.file.impl.metadata.FileEntityMetadata;
import com.github.silent.samurai.speedy.file.impl.metadata.FileFieldMetadata;
import com.github.silent.samurai.speedy.file.impl.util.JooqUtil;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.*;
import com.github.silent.samurai.speedy.interfaces.query.Condition;
import com.github.silent.samurai.speedy.utils.SpeedyValueFactory;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.util.*;


public class QueryBuilder {

    private final SpeedyQuery speedyQuery;
    private final FileEntityMetadata entityMetadata;
    private final DSLContext dslContext;
    private final List<FileEntityMetadata> joins = new LinkedList<>();
    private final SelectJoinStep<org.jooq.Record> query;

    public QueryBuilder(SpeedyQuery speedyQuery, DataSource dataSource, SQLDialect dialect) {
        this.speedyQuery = speedyQuery;
        this.entityMetadata = (FileEntityMetadata) speedyQuery.getFrom();
        this.dslContext = DSL.using(dataSource, dialect);
        this.query = dslContext.select()
                .from(DSL.table(entityMetadata.getName()));
    }

    Field<Object> getPath(BinaryCondition bCondition) {
        QueryField queryField = bCondition.getField();
        if (queryField.isAssociated()) {
//            FileFieldMetadata fieldMetadata = (FileFieldMetadata) queryField.getFieldMetadata();
            FileFieldMetadata associatedMetadata = (FileFieldMetadata) queryField.getAssociatedFieldMetadata();
            joins.add(associatedMetadata.getEntityMetadata());
            DataType<?> sqlDataType = JooqUtil.getSQLDataType(associatedMetadata.getValueType());
            return (Field<Object>) DSL.field(associatedMetadata.getDbColumnName(), sqlDataType);
        } else {
            FileFieldMetadata fieldMetadata = (FileFieldMetadata) queryField.getFieldMetadata();
            DataType<?> sqlDataType = JooqUtil.getSQLDataType(fieldMetadata.getValueType());
            return (Field<Object>) DSL.field(fieldMetadata.getDbColumnName(), sqlDataType);
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
                    .reduce(DSL.trueCondition(), org.jooq.Condition::or);
        }
        return predicates.stream()
                .reduce(DSL.trueCondition(), org.jooq.Condition::and);
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
            FileFieldMetadata fieldMetadata = (FileFieldMetadata) orderBy.getFieldMetadata();
            DataType<?> sqlDataType = JooqUtil.getSQLDataType(fieldMetadata.getValueType());
            Field<Object> field = (Field<Object>) DSL.field(
                    fieldMetadata.getDbColumnName(),
                    sqlDataType
            );
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
        query.limit(pageSize);
        query.offset(offset);
    }

    private void joins() {
        for (FileEntityMetadata join : joins) {
            query.join(join.getEntityType());
        }
    }

    public SelectJoinStep<Record> getQuery() throws Exception {
        if (Objects.nonNull(speedyQuery.getWhere())) {
            org.jooq.Condition whereCondition = conditionToPredicate(this.speedyQuery.getWhere());
            query.where(whereCondition);
        }
        captureOrderBy();
        addPageInfo();
        joins();
        return query;
    }


}
