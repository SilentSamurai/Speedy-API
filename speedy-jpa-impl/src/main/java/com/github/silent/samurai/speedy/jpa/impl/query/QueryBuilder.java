package com.github.silent.samurai.speedy.jpa.impl.query;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.enums.OrderByOperator;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.*;
import com.github.silent.samurai.speedy.models.*;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class QueryBuilder {

    private final SpeedyQuery speedyQuery;
    private final EntityMetadata entityMetadata;
    private final EntityManager entityManager;
    private final CriteriaBuilder criteriaBuilder;
    private final CriteriaQuery<?> query;
    private final Root<?> tableRoot;

    public QueryBuilder(SpeedyQuery speedyQuery, EntityManager entityManager) {
        this.speedyQuery = speedyQuery;
        this.entityMetadata = speedyQuery.getFrom();
        this.entityManager = entityManager;
        Objects.requireNonNull(entityMetadata, "Entity Not Found ");
        criteriaBuilder = entityManager.getCriteriaBuilder();
        query = criteriaBuilder.createQuery(entityMetadata.getEntityClass());
        tableRoot = query.from(entityMetadata.getEntityClass());
    }

    Predicate captureBooleanPredicate(BooleanCondition condition) throws Exception {
        List<Predicate> predicates = new ArrayList<>();
        for (Condition subCondition : condition.getConditions()) {
            Predicate predicate = conditionToPredicate(subCondition);
            predicates.add(predicate);
        }
        if (condition.getOperator() == ConditionOperator.OR) {
            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        }
        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }

    <T> Expression<T> getPath(BinaryCondition bCondition) {
        QueryField queryField = bCondition.getField();
        if (queryField.isAssociated()) {
            return tableRoot.get(queryField.getFieldMetadata().getClassFieldName())
                    .get(queryField.getAssociatedFieldMetadata().getClassFieldName());
        } else {
            return tableRoot.get(queryField.getFieldMetadata().getClassFieldName());
        }
    }

    Predicate equalPredicate(BinaryCondition bCondition) throws BadRequestException {
        SpeedyValue speedyValue = bCondition.getSpeedyValue();
        switch (speedyValue.getValueType()) {
            case BOOL: {
                Expression<? extends Comparable<Boolean>> path = getPath(bCondition);
                return criteriaBuilder.equal(path, speedyValue.asBoolean());
            }
            case TEXT: {
                Expression<? extends Comparable<String>> path = getPath(bCondition);
                SpeedyText value = (SpeedyText) speedyValue;
                return criteriaBuilder.equal(path, value.getValue());
            }
            case INT: {
                Expression<? extends Comparable<Integer>> path = getPath(bCondition);
                SpeedyInt value = (SpeedyInt) speedyValue;
                return criteriaBuilder.equal(path, value.getValue());
            }
            case FLOAT: {
                Expression<? extends Comparable<Double>> path = getPath(bCondition);
                SpeedyDouble value = (SpeedyDouble) speedyValue;
                return criteriaBuilder.equal(path, value.getValue());
            }
            case DATE: {
                Expression<? extends Comparable<LocalDate>> path = getPath(bCondition);
                SpeedyDate value = (SpeedyDate) speedyValue;
                return criteriaBuilder.equal(path, value.getValue());
            }
            case TIME: {
                Expression<? extends Comparable<LocalTime>> path = getPath(bCondition);
                SpeedyTime value = (SpeedyTime) speedyValue;
                return criteriaBuilder.equal(path, value.getValue());
            }
            case DATE_TIME: {
                Expression<? extends Comparable<LocalDateTime>> path = getPath(bCondition);
                SpeedyDateTime value = (SpeedyDateTime) speedyValue;
                return criteriaBuilder.equal(path, value.getValue());
            }
            case OBJECT:
            case COLLECTION:
                throw new BadRequestException("OBJECT & COLLECTION Operation not supported");
            case NULL: {
                Expression<? extends Comparable<LocalDateTime>> path = getPath(bCondition);
                return criteriaBuilder.isNull(path);
            }
        }
        return null;
    }

    Predicate notEqualPredicate(BinaryCondition bCondition) throws BadRequestException {
        SpeedyValue speedyValue = bCondition.getSpeedyValue();
        switch (speedyValue.getValueType()) {
            case BOOL: {
                Expression<? extends Comparable<Boolean>> path = getPath(bCondition);
                return criteriaBuilder.notEqual(path, speedyValue.asBoolean());
            }
            case TEXT: {
                Expression<? extends Comparable<String>> path = getPath(bCondition);
                SpeedyText value = (SpeedyText) speedyValue;
                return criteriaBuilder.notEqual(path, value.getValue());
            }
            case INT: {
                Expression<? extends Comparable<Integer>> path = getPath(bCondition);
                SpeedyInt value = (SpeedyInt) speedyValue;
                return criteriaBuilder.notEqual(path, value.getValue());
            }
            case FLOAT: {
                Expression<? extends Comparable<Double>> path = getPath(bCondition);
                SpeedyDouble value = (SpeedyDouble) speedyValue;
                return criteriaBuilder.notEqual(path, value.getValue());
            }
            case DATE: {
                Expression<? extends Comparable<LocalDate>> path = getPath(bCondition);
                SpeedyDate value = (SpeedyDate) speedyValue;
                return criteriaBuilder.notEqual(path, value.getValue());
            }
            case TIME: {
                Expression<? extends Comparable<LocalTime>> path = getPath(bCondition);
                SpeedyTime value = (SpeedyTime) speedyValue;
                return criteriaBuilder.notEqual(path, value.getValue());
            }
            case DATE_TIME: {
                Expression<? extends Comparable<LocalDateTime>> path = getPath(bCondition);
                SpeedyDateTime value = (SpeedyDateTime) speedyValue;
                return criteriaBuilder.notEqual(path, value.getValue());
            }
            case OBJECT:
            case COLLECTION:
                throw new BadRequestException("OBJECT & COLLECTION Operation not supported");
            case NULL: {
                Expression<? extends Comparable<LocalDateTime>> path = getPath(bCondition);
                return criteriaBuilder.isNotNull(path);
            }
        }
        return null;
    }

    Predicate lessThanPredicate(BinaryCondition bCondition) throws BadRequestException {
        SpeedyValue speedyValue = bCondition.getSpeedyValue();
        switch (speedyValue.getValueType()) {
            case BOOL: {
                Expression<Boolean> path = getPath(bCondition);
                return criteriaBuilder.lessThan(path, speedyValue.asBoolean());
            }
            case TEXT: {
                Expression<? extends String> path = getPath(bCondition);
                SpeedyText value = (SpeedyText) speedyValue;
                return criteriaBuilder.lessThan(path, value.getValue());
            }
            case INT: {
                Expression<? extends Integer> path = getPath(bCondition);
                SpeedyInt value = (SpeedyInt) speedyValue;
                return criteriaBuilder.lessThan(path, value.getValue());
            }
            case FLOAT: {
                Expression<? extends Double> path = getPath(bCondition);
                SpeedyDouble value = (SpeedyDouble) speedyValue;
                return criteriaBuilder.lessThan(path, value.getValue());
            }
            case DATE: {
                Expression<? extends LocalDate> path = getPath(bCondition);
                SpeedyDate value = (SpeedyDate) speedyValue;
                return criteriaBuilder.lessThan(path, value.getValue());
            }
            case TIME: {
                Expression<? extends LocalTime> path = getPath(bCondition);
                SpeedyTime value = (SpeedyTime) speedyValue;
                return criteriaBuilder.lessThan(path, value.getValue());
            }
            case DATE_TIME: {
                Expression<? extends LocalDateTime> path = getPath(bCondition);
                SpeedyDateTime value = (SpeedyDateTime) speedyValue;
                return criteriaBuilder.lessThan(path, value.getValue());
            }
            case OBJECT:
            case COLLECTION:
            case NULL:
                throw new BadRequestException("NULL, OBJECT & COLLECTION Operation not supported");
        }
        return null;
    }

    Predicate greaterThanPredicate(BinaryCondition bCondition) throws BadRequestException {
        SpeedyValue speedyValue = bCondition.getSpeedyValue();
        switch (speedyValue.getValueType()) {
            case BOOL: {
                Expression<? extends Boolean> path = getPath(bCondition);
                return criteriaBuilder.greaterThan(path, speedyValue.asBoolean());
            }
            case TEXT: {
                Expression<? extends String> path = getPath(bCondition);
                SpeedyText value = (SpeedyText) speedyValue;
                return criteriaBuilder.greaterThan(path, value.getValue());
            }
            case INT: {
                Expression<? extends Integer> path = getPath(bCondition);
                SpeedyInt value = (SpeedyInt) speedyValue;
                return criteriaBuilder.greaterThan(path, value.getValue());
            }
            case FLOAT: {
                Expression<? extends Double> path = getPath(bCondition);
                SpeedyDouble value = (SpeedyDouble) speedyValue;
                return criteriaBuilder.greaterThan(path, value.getValue());
            }
            case DATE: {
                Expression<? extends LocalDate> path = getPath(bCondition);
                SpeedyDate value = (SpeedyDate) speedyValue;
                return criteriaBuilder.greaterThan(path, value.getValue());
            }
            case TIME: {
                Expression<? extends LocalTime> path = getPath(bCondition);
                SpeedyTime value = (SpeedyTime) speedyValue;
                return criteriaBuilder.greaterThan(path, value.getValue());
            }
            case DATE_TIME: {
                Expression<? extends LocalDateTime> path = getPath(bCondition);
                SpeedyDateTime value = (SpeedyDateTime) speedyValue;
                return criteriaBuilder.greaterThan(path, value.getValue());
            }
            case OBJECT:
            case COLLECTION:
            case NULL:
                throw new BadRequestException("NULL, OBJECT & COLLECTION Operation not supported");
        }
        return null;
    }

    Predicate lessThanOrEqualToPredicate(BinaryCondition bCondition) throws BadRequestException {
        SpeedyValue speedyValue = bCondition.getSpeedyValue();
        switch (speedyValue.getValueType()) {
            case BOOL: {
                Expression<? extends Boolean> path = getPath(bCondition);
                return criteriaBuilder.lessThanOrEqualTo(path, speedyValue.asBoolean());
            }
            case TEXT: {
                Expression<? extends String> path = getPath(bCondition);
                return criteriaBuilder.lessThanOrEqualTo(path, speedyValue.asText());
            }
            case INT: {
                Expression<? extends Integer> path = getPath(bCondition);
                SpeedyInt value = (SpeedyInt) speedyValue;
                return criteriaBuilder.lessThanOrEqualTo(path, value.getValue());
            }
            case FLOAT: {
                Expression<? extends Double> path = getPath(bCondition);
                SpeedyDouble value = (SpeedyDouble) speedyValue;
                return criteriaBuilder.lessThanOrEqualTo(path, value.getValue());
            }
            case DATE: {
                Expression<? extends LocalDate> path = getPath(bCondition);
                SpeedyDate value = (SpeedyDate) speedyValue;
                return criteriaBuilder.lessThanOrEqualTo(path, value.getValue());
            }
            case TIME: {
                Expression<? extends LocalTime> path = getPath(bCondition);
                SpeedyTime value = (SpeedyTime) speedyValue;
                return criteriaBuilder.lessThanOrEqualTo(path, value.getValue());
            }
            case DATE_TIME: {
                Expression<? extends LocalDateTime> path = getPath(bCondition);
                SpeedyDateTime value = (SpeedyDateTime) speedyValue;
                return criteriaBuilder.lessThanOrEqualTo(path, value.getValue());
            }
            case OBJECT:
            case COLLECTION:
            case NULL:
                throw new BadRequestException("NULL, OBJECT & COLLECTION Operation not supported");
        }
        return null;
    }

    Predicate greaterThanOrEqualToPredicate(BinaryCondition bCondition) throws BadRequestException {
        SpeedyValue speedyValue = bCondition.getSpeedyValue();
        switch (speedyValue.getValueType()) {
            case BOOL: {
                Expression<? extends Boolean> path = getPath(bCondition);
                Boolean aBoolean = speedyValue.asBoolean();
                return criteriaBuilder.greaterThanOrEqualTo(path, aBoolean);
            }
            case TEXT: {
                Expression<? extends String> path = getPath(bCondition);
                SpeedyText value = (SpeedyText) speedyValue;
                return criteriaBuilder.greaterThanOrEqualTo(path, value.getValue());
            }
            case INT: {
                Expression<? extends Integer> path = getPath(bCondition);
                SpeedyInt value = (SpeedyInt) speedyValue;
                return criteriaBuilder.greaterThanOrEqualTo(path, value.getValue());
            }
            case FLOAT: {
                Expression<? extends Double> path = getPath(bCondition);
                SpeedyDouble value = (SpeedyDouble) speedyValue;
                return criteriaBuilder.greaterThanOrEqualTo(path, value.getValue());
            }
            case DATE: {
                Expression<? extends LocalDate> path = getPath(bCondition);
                SpeedyDate value = (SpeedyDate) speedyValue;
                return criteriaBuilder.greaterThanOrEqualTo(path, value.getValue());
            }
            case TIME: {
                Expression<? extends LocalTime> path = getPath(bCondition);
                SpeedyTime value = (SpeedyTime) speedyValue;
                return criteriaBuilder.greaterThanOrEqualTo(path, value.getValue());
            }
            case DATE_TIME: {
                Expression<? extends LocalDateTime> path = getPath(bCondition);
                SpeedyDateTime value = (SpeedyDateTime) speedyValue;
                return criteriaBuilder.greaterThanOrEqualTo(path, value.getValue());
            }
            case OBJECT:
            case COLLECTION:
            case NULL:
            default:
                throw new BadRequestException("NULL, OBJECT & COLLECTION Operation not supported");
        }
    }

    Predicate inPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        SpeedyValue speedyValue = bCondition.getSpeedyValue();
        switch (speedyValue.getValueType()) {
            case BOOL: {
                Expression<? extends String> path = getPath(bCondition);
                Boolean aBoolean = speedyValue.asBoolean();
                return path.in(aBoolean);
            }
            case TEXT: {
                Expression<? extends String> path = getPath(bCondition);
                SpeedyText value = (SpeedyText) speedyValue;
                return path.in(value.getValue());
            }
            case INT: {
                Expression<? extends Integer> path = getPath(bCondition);
                SpeedyInt value = (SpeedyInt) speedyValue;
                return path.in(value.getValue());
            }
            case FLOAT: {
                Expression<? extends Double> path = getPath(bCondition);
                SpeedyDouble value = (SpeedyDouble) speedyValue;
                return path.in(value.getValue());
            }
            case DATE: {
                Expression<? extends LocalDate> path = getPath(bCondition);
                SpeedyDate value = (SpeedyDate) speedyValue;
                return path.in(value.getValue());
            }
            case TIME: {
                Expression<? extends LocalTime> path = getPath(bCondition);
                SpeedyTime value = (SpeedyTime) speedyValue;
                return path.in(value.getValue());
            }
            case DATE_TIME: {
                Expression<? extends LocalDateTime> path = getPath(bCondition);
                SpeedyDateTime value = (SpeedyDateTime) speedyValue;
                return path.in(value.getValue());
            }
            case COLLECTION: {
                FieldMetadata fieldMetadata = bCondition.getField().getFieldMetadata();
                if (!fieldMetadata.isAssociation()) {
                    Expression<? extends Comparable> path = getPath(bCondition);
                    Collection<SpeedyValue> collection = speedyValue.asCollection();
                    Collection<Object> objects = new ArrayList<>(collection.size());
                    for (SpeedyValue sv : collection) {
                        Object rawValue = SpeedyValueFactory.speedyValueToJavaType(sv, fieldMetadata.getFieldType());
                        objects.add(rawValue);
                    }
                    return path.in(objects);
                }
                throw new BadRequestException("COLLECTION of Association Operation not supported");
            }
            case OBJECT:
            case NULL:
            default:
                throw new BadRequestException("NULL, OBJECT Operation not supported");
        }
    }

    Predicate notInPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        SpeedyValue speedyValue = bCondition.getSpeedyValue();
        switch (speedyValue.getValueType()) {
            case TEXT: {
                Expression<? extends String> path = getPath(bCondition);
                SpeedyText value = (SpeedyText) speedyValue;
                return criteriaBuilder.not(path.in(value.getValue()));
            }
            case INT: {
                Expression<? extends Integer> path = getPath(bCondition);
                SpeedyInt value = (SpeedyInt) speedyValue;
                return criteriaBuilder.not(path.in(value.getValue()));
            }
            case FLOAT: {
                Expression<? extends Double> path = getPath(bCondition);
                SpeedyDouble value = (SpeedyDouble) speedyValue;
                return criteriaBuilder.not(path.in(value.getValue()));
            }
            case DATE: {
                Expression<? extends LocalDate> path = getPath(bCondition);
                SpeedyDate value = (SpeedyDate) speedyValue;
                return criteriaBuilder.not(path.in(value.getValue()));
            }
            case TIME: {
                Expression<? extends LocalTime> path = getPath(bCondition);
                SpeedyTime value = (SpeedyTime) speedyValue;
                return criteriaBuilder.not(path.in(value.getValue()));
            }
            case DATE_TIME: {
                Expression<? extends LocalDateTime> path = getPath(bCondition);
                SpeedyDateTime value = (SpeedyDateTime) speedyValue;
                return criteriaBuilder.not(path.in(value.getValue()));
            }
            case COLLECTION: {
                FieldMetadata fieldMetadata = bCondition.getField().getFieldMetadata();
                if (!fieldMetadata.isAssociation()) {
                    Expression<? extends Comparable> path = getPath(bCondition);
                    Collection<SpeedyValue> collection = speedyValue.asCollection();
                    Collection<Object> objects = new ArrayList<>(collection.size());
                    for (SpeedyValue sv : collection) {
                        Object rawValue = SpeedyValueFactory.speedyValueToJavaType(sv, fieldMetadata.getFieldType());
                        objects.add(rawValue);
                    }
                    return criteriaBuilder.not(path.in(objects));
                }
                throw new BadRequestException("COLLECTION of Association Operation not supported");
            }
            case OBJECT:
            case NULL:
                throw new BadRequestException("NULL, OBJECT & COLLECTION Operation not supported");
        }
        return null;
    }

    Predicate conditionToPredicate(Condition condition) throws Exception {
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

    List<Order> captureOrderBy() {
        List<Order> orderList = new LinkedList<>();
        for (OrderBy orderBy : speedyQuery.getOrderByList()) {
            String classFieldName = orderBy.getFieldMetadata().getClassFieldName();
            OrderByOperator operator = orderBy.getOperator();
            if (operator == OrderByOperator.ASC) {
                orderList.add(criteriaBuilder.asc(tableRoot.get(classFieldName)));
            }
            if (operator == OrderByOperator.DESC) {
                orderList.add(criteriaBuilder.desc(tableRoot.get(classFieldName)));
            }
        }
        return orderList;
    }

    private TypedQuery<?> addPageInfo() throws BadRequestException {
        TypedQuery<?> pagedQuery = entityManager.createQuery(query);
        PageInfo pageInfo = speedyQuery.getPageInfo();
        int pageSize = pageInfo.getPageSize();
        int pageNumber = pageInfo.getPageNo();
        pagedQuery.setMaxResults(pageSize);
        pagedQuery.setFirstResult(pageSize * pageNumber);
        return pagedQuery;
    }

    public Query getQuery() throws Exception {
        if (Objects.nonNull(speedyQuery.getWhere())) {
            Predicate whereCondition = conditionToPredicate(this.speedyQuery.getWhere());
            query.where(whereCondition);
        }
        query.orderBy(captureOrderBy());
        return addPageInfo();
    }


}
