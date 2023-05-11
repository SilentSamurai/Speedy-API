package com.github.silent.samurai.models.conditions;

import com.fasterxml.jackson.core.JsonParser;
import com.github.silent.samurai.models.Operator;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;


public interface Condition {

    Operator getOperator();

    void updateFromJson(JsonParser jsonParser);


    Predicate getPredicate(CriteriaBuilder criteriaBuilder,
                           Root<?> tableRoot,
                           EntityMetadata entityMetadata) throws Exception;


}
