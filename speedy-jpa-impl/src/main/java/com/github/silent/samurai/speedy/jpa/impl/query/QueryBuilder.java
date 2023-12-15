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
import com.github.silent.samurai.speedy.utils.SpeedyValueFactory;
import com.google.common.collect.Lists;

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

    Object getRawValue(BinaryCondition bCondition, SpeedyValue speedyValue) throws SpeedyHttpException {
        QueryField queryField = bCondition.getField();
        if (queryField.isAssociated()) {
            return SpeedyValueFactory.toJavaType(queryField.getAssociatedFieldMetadata(), speedyValue);
        }
        return SpeedyValueFactory.toJavaType(bCondition.getField().getFieldMetadata(), speedyValue);
    }

    Predicate equalPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        SpeedyValue speedyValue = bCondition.getSpeedyValue();
        if (!speedyValue.isValue()) {
            throw new BadRequestException("OBJECT & COLLECTION Operation not supported");
        }
        if (speedyValue.isNull()) {
            Expression<? extends Comparable<?>> path = getPath(bCondition);
            return criteriaBuilder.isNull(path);
        } else {
            Expression<? extends Comparable<?>> path = getPath(bCondition);
            Object rawValue = getRawValue(bCondition, speedyValue);
            return criteriaBuilder.equal(path, rawValue);
        }
    }

    Predicate notEqualPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        SpeedyValue speedyValue = bCondition.getSpeedyValue();
        if (!speedyValue.isValue()) {
            throw new BadRequestException("OBJECT & COLLECTION Operation not supported");
        }
        if (speedyValue.isNull()) {
            Expression<? extends Comparable<?>> path = getPath(bCondition);
            return criteriaBuilder.isNotNull(path);
        } else {
            Expression<? extends Comparable<?>> path = getPath(bCondition);
            Object rawValue = getRawValue(bCondition, speedyValue);
            return criteriaBuilder.notEqual(path, rawValue);
        }
    }

    Predicate lessThanPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        SpeedyValue speedyValue = bCondition.getSpeedyValue();
        if (!speedyValue.isValue()) {
            throw new BadRequestException("OBJECT & COLLECTION Operation not supported");
        }
        Expression<? extends Comparable<Object>> path = getPath(bCondition);
        Object rawValue = getRawValue(bCondition, speedyValue);
        return criteriaBuilder.lessThan(path, (Comparable<Object>) rawValue);
    }

    Predicate greaterThanPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        SpeedyValue speedyValue = bCondition.getSpeedyValue();
        if (!speedyValue.isValue()) {
            throw new BadRequestException("OBJECT & COLLECTION Operation not supported");
        }
        Expression<? extends Comparable<Object>> path = getPath(bCondition);
        Object rawValue = getRawValue(bCondition, speedyValue);
        return criteriaBuilder.greaterThan(path, (Comparable<Object>) rawValue);
    }

    Predicate lessThanOrEqualToPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        SpeedyValue speedyValue = bCondition.getSpeedyValue();
        if (!speedyValue.isValue()) {
            throw new BadRequestException("OBJECT & COLLECTION Operation not supported");
        }
        Expression<? extends Comparable<Object>> path = getPath(bCondition);
        Object rawValue = getRawValue(bCondition, speedyValue);
        return criteriaBuilder.lessThanOrEqualTo(path, (Comparable<Object>) rawValue);
    }

    Predicate greaterThanOrEqualToPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        SpeedyValue speedyValue = bCondition.getSpeedyValue();
        if (!speedyValue.isValue()) {
            throw new BadRequestException("OBJECT & COLLECTION Operation not supported");
        }
        Expression<? extends Comparable<Object>> path = getPath(bCondition);
        Object rawValue = getRawValue(bCondition, speedyValue);
        return criteriaBuilder.greaterThanOrEqualTo(path, (Comparable<Object>) rawValue);
    }

    Predicate inPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        SpeedyValue speedyValue = bCondition.getSpeedyValue();
        if (speedyValue.isCollection()) {
            FieldMetadata fieldMetadata = bCondition.getField().getFieldMetadata();
            if (!fieldMetadata.isAssociation()) {
                Expression<? extends Comparable> path = getPath(bCondition);
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
        Expression<? extends Comparable<?>> path = getPath(bCondition);
        Object rawValue = getRawValue(bCondition, speedyValue);
        return path.in(rawValue);
    }

    Predicate notInPredicate(BinaryCondition bCondition) throws SpeedyHttpException {
        SpeedyValue speedyValue = bCondition.getSpeedyValue();
        if (speedyValue.isCollection()) {
            FieldMetadata fieldMetadata = bCondition.getField().getFieldMetadata();
            if (!fieldMetadata.isAssociation()) {
                Expression<? extends Comparable> path = getPath(bCondition);
                Collection<SpeedyValue> collection = speedyValue.asCollection();
                Collection<Object> objects = new ArrayList<>(collection.size());
                for (SpeedyValue sv : collection) {
                    Object rawValue = getRawValue(bCondition, sv);
                    objects.add(rawValue);
                }
                return criteriaBuilder.not(path.in(objects));
            }
            throw new BadRequestException("COLLECTION of Association Operation not supported");
        }
        if (!speedyValue.isValue()) {
            throw new BadRequestException("OBJECT Operation not supported");
        }
        Expression<? extends Comparable<?>> path = getPath(bCondition);
        Object rawValue = getRawValue(bCondition, speedyValue);
        return criteriaBuilder.not(path.in(List.of(rawValue)));
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
