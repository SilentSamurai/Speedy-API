package com.github.silent.samurai.models.conditions;

import com.github.silent.samurai.exceptions.SpeedyHttpException;
import com.github.silent.samurai.interfaces.FieldMetadata;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

public interface DbField {

    FieldMetadata getFieldMetadata();

    <T> Path<T> getPath(CriteriaBuilder criteriaBuilder,
                        Root<?> tableRoot) throws SpeedyHttpException;


}
