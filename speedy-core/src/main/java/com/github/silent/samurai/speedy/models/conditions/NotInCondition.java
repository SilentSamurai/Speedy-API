package com.github.silent.samurai.speedy.models.conditions;

import com.fasterxml.jackson.core.JsonParser;
import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import lombok.Data;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;

@Data
public class NotInCondition implements BinaryMVCondition {

    private DbField field;
    private ConditionOperator conditionOperator = ConditionOperator.NOT_IN;
    private List<Object> instances;

    public NotInCondition(DbField field, List<Object> instances) {
        this.field = field;
        this.instances = instances;
    }

    @Override
    public void updateFromJson(JsonParser jsonParser) {
    }

    @Override
    public Predicate getPredicate(CriteriaBuilder criteriaBuilder, Root<?> tableRoot, EntityMetadata entityMetadata) throws Exception {
        Path<? extends Comparable<?>> path = field.getPath(criteriaBuilder, tableRoot);
        return criteriaBuilder.not(path.in(instances));
    }
}
