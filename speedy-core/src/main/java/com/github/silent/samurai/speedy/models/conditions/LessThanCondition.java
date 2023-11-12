package com.github.silent.samurai.speedy.models.conditions;

import com.fasterxml.jackson.core.JsonParser;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.models.Operator;
import lombok.Data;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

@Data
public class LessThanCondition implements BinarySVCondition {

    private DbField field;
    private Object instance;
    private Operator operator = Operator.LT;

    public LessThanCondition(DbField field, Object instance) {
        this.field = field;
        this.instance = instance;
    }

    @Override
    public void updateFromJson(JsonParser jsonParser) {
    }

    @Override
    public Predicate getPredicate(CriteriaBuilder criteriaBuilder, Root<?> tableRoot, EntityMetadata entityMetadata) throws Exception {
        return criteriaBuilder.lessThan(field.getPath(criteriaBuilder, tableRoot), (Comparable) instance);
    }
}
