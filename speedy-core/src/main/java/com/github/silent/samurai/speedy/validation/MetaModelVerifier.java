package com.github.silent.samurai.speedy.validation;


import com.github.silent.samurai.speedy.enums.EtagStrategy;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModel;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class MetaModelVerifier {


    private static final Set<ValueType> TEMPORAL_TYPES = Set.of(
            ValueType.DATE, ValueType.TIME, ValueType.DATE_TIME, ValueType.ZONED_DATE_TIME);
    private final MetaModel metaModel;

    public MetaModelVerifier(MetaModel metaModel) {
        this.metaModel = metaModel;
    }

    public void verify() throws SpeedyHttpException {
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

            verifyEtagField(entityMetadata);
        }
    }

    /// Fails fast if a {@code @SpeedyETag} field's value type doesn't fit its strategy — e.g.
    /// {@code RANDOM} (writes a UUID string) on a numeric column, or {@code TIMESTAMP} on a
    /// non-temporal one. Left unchecked, the mismatch would only surface as a write-time
    /// conversion failure on the first create/update of the entity. Also rejects a second
    /// {@code @SpeedyETag} field on the same entity: {@link EntityMetadata#getVersionField()}
    /// returns an arbitrary one of them (backed by an unordered field map), so a second
    /// declaration is silently non-deterministic rather than a clean error unless caught here.
    private void verifyEtagField(EntityMetadata entityMetadata) throws SpeedyHttpException {
        long versionFieldCount = entityMetadata.getAllFields().stream().filter(f -> f.getEtagStrategy().isPresent()).count();
        if (versionFieldCount > 1) {
            throw new InternalServerError(String.format(
                    "entity %s declares %d @SpeedyETag/@Version fields; at most one is supported",
                    entityMetadata.getName(), versionFieldCount));
        }

        Optional<FieldMetadata> versionField = entityMetadata.getVersionField();
        if (versionField.isEmpty()) {
            return;
        }
        FieldMetadata etagField = versionField.get();
        EtagStrategy strategy = etagField.getEtagStrategy().get();
        boolean valid = switch (strategy) {
            case RANDOM -> etagField.getValueType() == ValueType.TEXT;
            case TIMESTAMP -> TEMPORAL_TYPES.contains(etagField.getValueType());
        };
        if (!valid) {
            throw new InternalServerError(String.format(
                    "@SpeedyETag field %s.%s has strategy %s, which does not fit its value type %s",
                    entityMetadata.getName(), etagField.getOutputPropertyName(), strategy, etagField.getValueType()));
        }
    }


}
