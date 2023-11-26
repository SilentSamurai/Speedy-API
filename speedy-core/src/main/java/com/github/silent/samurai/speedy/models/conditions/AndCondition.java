package com.github.silent.samurai.speedy.models.conditions;

import com.fasterxml.jackson.core.JsonParser;
import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import lombok.Data;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

@Data
public class AndCondition implements Condition {

    private ConditionOperator conditionOperator;
    private List<Condition> conditions;

    @Override
    public void updateFromJson(JsonParser jsonParser) {

    }

    @Override
    public Predicate getPredicate(CriteriaBuilder criteriaBuilder, Root<?> tableRoot, EntityMetadata entityMetadata) throws Exception {
        List<Predicate> list = new ArrayList<>();
        for (Condition condition : conditions) {
            Predicate predicate = condition.getPredicate(criteriaBuilder, tableRoot, entityMetadata);
            list.add(predicate);
        }
        Predicate[] predicates = list.toArray(new Predicate[0]);
        return criteriaBuilder.and(predicates);
    }


}
