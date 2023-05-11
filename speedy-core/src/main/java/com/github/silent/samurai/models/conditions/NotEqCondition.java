package com.github.silent.samurai.models.conditions;

import com.fasterxml.jackson.core.JsonParser;
import com.github.silent.samurai.models.Operator;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import lombok.Data;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

@Data
public class NotEqCondition implements BinarySVCondition {

    private DbField field;
    private Object instance;
    private Operator operator = Operator.NEQ;

    public NotEqCondition(DbField field, Object instance) {
        this.field = field;
        this.instance = instance;
    }

    @Override
    public void updateFromJson(JsonParser jsonParser) {
    }

    @Override
    public Predicate getPredicate(CriteriaBuilder criteriaBuilder, Root<?> tableRoot, EntityMetadata entityMetadata) throws Exception {
        Path<? extends Comparable<?>> path = field.getPath(criteriaBuilder, tableRoot);
        return criteriaBuilder.notEqual(path, instance);
    }
}
