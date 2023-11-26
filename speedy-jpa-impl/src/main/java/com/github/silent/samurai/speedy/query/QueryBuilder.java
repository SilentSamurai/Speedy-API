package com.github.silent.samurai.speedy.query;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.enums.OrderByOperator;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.query.*;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

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

    Predicate conditionToPredicate(Condition condition) throws Exception {
        ConditionOperator operator = condition.getOperator();
        if (operator == ConditionOperator.AND || operator == ConditionOperator.OR) {
            return captureBooleanPredicate((BooleanCondition) condition);
        }

        BinaryCondition bCondition = (BinaryCondition) condition;
        Field field = bCondition.getField();
        Expression<? extends Comparable> path;
        if (field.hasAssociatedFieldMetadata()) {
            path = tableRoot.get(field.getFieldMetadata().getClassFieldName())
                    .get(field.getAssociatedFieldMetadata().getClassFieldName());
        } else {
            path = tableRoot.get(field.getFieldMetadata().getClassFieldName());
        }

        switch (condition.getOperator()) {
            case EQ:
                return criteriaBuilder.equal(path, bCondition.getSpeedyValue().getSingleValue());
            case NEQ:
                return criteriaBuilder.notEqual(path, bCondition.getSpeedyValue().getSingleValue());
            case LT:
                return criteriaBuilder.lessThan(path,
                        (Comparable) bCondition.getSpeedyValue().getSingleValue());
            case GT:
                return criteriaBuilder.greaterThan(path,
                        (Comparable) bCondition.getSpeedyValue().getSingleValue());
            case LTE:
                return criteriaBuilder.lessThanOrEqualTo(path,
                        (Comparable) bCondition.getSpeedyValue().getSingleValue());
            case GTE:
                return criteriaBuilder.greaterThanOrEqualTo(path,
                        (Comparable) bCondition.getSpeedyValue().getSingleValue());
            case IN:
                return path.in(bCondition.getSpeedyValue().getValues());
            case NOT_IN:
                return criteriaBuilder.not(path.in(bCondition.getSpeedyValue().getValues()));
            default:
                throw new BadRequestException("Unknown Operator");
        }
    }

    List<Order> captureOrderBy() {
        List<Order> orderList = new LinkedList<>();
        for (OrderBy orderBy : speedyQuery.getOrderBy()) {
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
