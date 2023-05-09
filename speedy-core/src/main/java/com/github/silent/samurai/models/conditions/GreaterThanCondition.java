package com.github.silent.samurai.models.conditions;

import com.fasterxml.jackson.core.JsonParser;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.models.Operator;
import lombok.Data;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

@Data
public class GreaterThanCondition implements BinarySVCondition {

    private DbField field;
    private Object instance;
    private Operator operator = Operator.GT;

    public GreaterThanCondition(DbField field, Object instance) {
        this.field = field;
        this.instance = instance;
    }

    @Override
    public void updateFromJson(JsonParser jsonParser) {
    }

    @Override
    public Predicate getPredicate(CriteriaBuilder criteriaBuilder,
                                  Root<?> tableRoot,
                                  EntityMetadata entityMetadata) throws Exception {
        return criteriaBuilder.greaterThan(field.getPath(criteriaBuilder, tableRoot), (Comparable) instance);
    }
}
