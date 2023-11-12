package com.github.silent.samurai.speedy.models.conditions;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import lombok.Getter;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

@Getter
public class NormalField implements DbField {

    private final FieldMetadata fieldMetadata;

    public NormalField(FieldMetadata fieldMetadata) {
        this.fieldMetadata = fieldMetadata;
    }

    @Override
    public <T> Path<T> getPath(CriteriaBuilder criteriaBuilder,
                               Root<?> tableRoot) throws SpeedyHttpException {
        String name = fieldMetadata.getClassFieldName();
        return tableRoot.get(name);
    }

}
