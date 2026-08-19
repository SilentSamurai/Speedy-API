package com.github.silent.samurai.speedy.validation;


import com.github.silent.samurai.speedy.enums.EtagStrategy;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.metadata.AssociationColumn;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModel;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
                    verifyAssociationCoversTargetKey(entityMetadata, fieldMetadata);
                    verifyKeyFieldAssociationIsSingleColumn(entityMetadata, fieldMetadata);
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

    /// Fails fast unless a to-one association is mapped through *every* column of its target's
    /// primary key. A foreign key covering only part of a composite key cannot identify a single
    /// target row: reads would populate a partial {@code SpeedyEntityKey} and resolve to whichever
    /// sibling row the database returned first, and writes would leave the remaining key columns
    /// unset. Only a metamodel built outside the JPA processor can express this (by hand via
    /// {@code MetadataBuilder}, or from JSON via {@code FileProcessor}) — the JPA processor rejects
    /// it while reading the annotations — so the check lives here, where every metamodel source
    /// passes through it.
    ///
    /// Collection associations are exempt: they are mapped by the *inverse* side's field
    /// ({@code @OneToMany(mappedBy)}), not by a foreign key on this entity.
    private void verifyAssociationCoversTargetKey(EntityMetadata entityMetadata, FieldMetadata fieldMetadata)
            throws SpeedyHttpException {
        if (fieldMetadata.isCollection()) {
            return;
        }
        Set<FieldMetadata> mapped = fieldMetadata.getAssociationColumns().stream()
                .map(AssociationColumn::targetKeyField)
                .collect(Collectors.toSet());
        Set<FieldMetadata> targetKey = Set.copyOf(fieldMetadata.getAssociationMetadata().getKeyFields());
        if (mapped.equals(targetKey)) {
            return;
        }
        throw new InternalServerError(String.format(
                "association %s.%s references %s through %d column(s) [%s], but %s has a %d-column primary key [%s] — " +
                        "an association must be mapped through every key column of its target",
                entityMetadata.getName(), fieldMetadata.getOutputPropertyName(),
                fieldMetadata.getAssociationMetadata().getName(),
                mapped.size(), joinColumnNames(fieldMetadata),
                fieldMetadata.getAssociationMetadata().getName(),
                targetKey.size(), joinFieldNames(targetKey)));
    }

    /// Fails fast if one of an entity's *own* key fields is a multi-column foreign key. Speedy's
    /// primary-key paths address each key field as a single column — the {@code WHERE} clauses
    /// behind get-by-key, update and delete, and the key extraction that feeds them — so such a
    /// field would silently match on its first column alone and act on the wrong rows.
    ///
    /// A multi-column foreign key is perfectly fine as an ordinary field; it is only being part of
    /// the *owning* entity's key that is unsupported.
    private void verifyKeyFieldAssociationIsSingleColumn(EntityMetadata entityMetadata, FieldMetadata fieldMetadata)
            throws SpeedyHttpException {
        if (!(fieldMetadata instanceof KeyFieldMetadata) || !fieldMetadata.isCompositeAssociation()) {
            return;
        }
        throw new InternalServerError(String.format(
                "key field %s.%s is a foreign key spanning %d columns [%s] — a key field must map to a " +
                        "single column, because get-by-key, update and delete address it as one",
                entityMetadata.getName(), fieldMetadata.getOutputPropertyName(),
                fieldMetadata.getAssociationColumns().size(), joinColumnNames(fieldMetadata)));
    }

    private static String joinColumnNames(FieldMetadata fieldMetadata) {
        return fieldMetadata.getAssociationColumns().stream()
                .map(c -> c.localDbColumnName() + " -> " + c.targetKeyField().getOutputPropertyName())
                .collect(Collectors.joining(", "));
    }

    private static String joinFieldNames(Set<FieldMetadata> fields) {
        return fields.stream()
                .map(FieldMetadata::getOutputPropertyName)
                .collect(Collectors.joining(", "));
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
