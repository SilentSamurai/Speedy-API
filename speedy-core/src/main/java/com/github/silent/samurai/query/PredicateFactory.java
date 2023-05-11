package com.github.silent.samurai.query;

import com.github.silent.samurai.models.Operator;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Collection;

public class PredicateFactory {

    final CriteriaBuilder cb;
    final Root<?> root;

    public PredicateFactory(CriteriaBuilder cb, Root<?> root) {
        this.cb = cb;
        this.root = root;
    }

    public Predicate create(Operator operator, String fieldName, Object fieldValue) throws BadRequestException {
        Path<Comparable> path = root.get(fieldName);

        switch (operator) {
            case EQ:
                return cb.equal(path, fieldValue);
            case NEQ:
                return cb.notEqual(path, fieldValue);
            case LT:
                return cb.lessThan(path, (Comparable) fieldValue);
            case GT:
                return cb.greaterThan(path, (Comparable) fieldValue);
            case LTE:
                return cb.lessThanOrEqualTo(path, (Comparable) fieldValue);
            case GTE:
                return cb.greaterThanOrEqualTo(path, (Comparable) fieldValue);
            case IN:
                if (fieldValue instanceof Collection) {
                    return path.in((Collection<?>) fieldValue);
                } else {
                    return path.in(fieldValue);
                }
            case NOT_IN:
                if (fieldValue instanceof Collection) {
                    return cb.not(path.in((Collection<?>) fieldValue));
                } else {
                    return cb.not(path.in(fieldValue));
                }
            default:
                throw new BadRequestException("Unsupported operator: " + operator);
        }
    }

    public Predicate createBetween(String fieldName, Object startValue, Object endValue) {
        Path<? extends Comparable> path = root.get(fieldName);
        return this.cb.between(path, (Comparable) startValue, (Comparable) endValue);
    }

}
