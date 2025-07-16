package com.github.silent.samurai.speedy.validation;


import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModel;

import java.util.Objects;

public class MetaModelVerifier {


    private final MetaModel metaModel;

    public MetaModelVerifier(MetaModel metaModel) {
        this.metaModel = metaModel;
    }

    public boolean verify() throws SpeedyHttpException {
        for (EntityMetadata entityMetadata : metaModel.getAllEntityMetadata()) {
            Objects.requireNonNull(entityMetadata);
            Objects.requireNonNull(entityMetadata.getName(), "Entity Name not found");
            Objects.requireNonNull(entityMetadata.getDbTableName(), entityMetadata.getName() + " Db Table Name not found");

            for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
                Objects.requireNonNull(fieldMetadata);
                Objects.requireNonNull(fieldMetadata.getValueType());
                Objects.requireNonNull(fieldMetadata.getDbColumnName(), entityMetadata.getName() + " Db Column Name not found");

                if (fieldMetadata.isAssociation()) {
                    Objects.requireNonNull(fieldMetadata.getAssociationMetadata());
                    Objects.requireNonNull(fieldMetadata.getAssociatedFieldMetadata());
                }

                if (fieldMetadata.getValueType() == ValueType.OBJECT || fieldMetadata.getValueType() == ValueType.COLLECTION) {
                    if (!fieldMetadata.isAssociation()) {
                        String msg = String.format(
                                "field %s in entity %s is derived as speedy object type which is not supported",
                                fieldMetadata.getOutputPropertyName(), entityMetadata.getName());
                        throw new InternalServerError(msg);
                    }
                }


            }

        }
        return true;
    }


}
