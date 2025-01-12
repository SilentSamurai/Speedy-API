package com.github.silent.samurai.speedy.validation;


import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;

import java.util.Objects;

public class MetaModelVerifier {


    private final MetaModelProcessor metaModelProcessor;

    public MetaModelVerifier(MetaModelProcessor metaModelProcessor) {
        this.metaModelProcessor = metaModelProcessor;
    }

    public boolean verify() {
        for (EntityMetadata entityMetadata : metaModelProcessor.getAllEntityMetadata()) {
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

            }

        }
        return true;
    }


}
