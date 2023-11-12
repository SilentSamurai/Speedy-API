package com.github.silent.samurai.speedy.models.conditions;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import lombok.Getter;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

@Getter
public class AssociatedField implements DbField {

    private final FieldMetadata fieldMetadata;
    private final FieldMetadata parentFieldMetadata;

    public AssociatedField(FieldMetadata parentField, FieldMetadata fieldMetadata) {
        this.fieldMetadata = fieldMetadata;
        this.parentFieldMetadata = parentField;
    }

    @Override
    public <T> Path<T> getPath(CriteriaBuilder criteriaBuilder,
                               Root<?> tableRoot) throws SpeedyHttpException {
        return tableRoot.get(parentFieldMetadata.getClassFieldName())
                .get(fieldMetadata.getClassFieldName());
    }

}
