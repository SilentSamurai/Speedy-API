package com.github.silent.samurai.speedy.models.conditions;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

public interface DbField {
    FieldMetadata getFieldMetadata();

    <T> Path<T> getPath(CriteriaBuilder criteriaBuilder,
                        Root<?> tableRoot) throws SpeedyHttpException;


}
