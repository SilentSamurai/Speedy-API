package com.github.silent.samurai.speedy.models.conditions;

import com.fasterxml.jackson.core.JsonParser;
import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import lombok.Data;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

@Data
public class EqCondition implements BinarySVCondition {

    private DbField field;
    private Object instance;
    private ConditionOperator conditionOperator = ConditionOperator.EQ;

    public EqCondition(DbField field, Object instance) {
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
        Path<? extends Comparable<?>> path = field.getPath(criteriaBuilder, tableRoot);
        return criteriaBuilder.equal(path, instance);
    }
}
