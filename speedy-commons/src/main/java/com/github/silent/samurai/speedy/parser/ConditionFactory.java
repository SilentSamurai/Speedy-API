package com.github.silent.samurai.speedy.parser;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.Expression;
import com.github.silent.samurai.speedy.interfaces.query.QueryField;
import com.github.silent.samurai.speedy.models.conditions.BinaryConditionImpl;
import com.github.silent.samurai.speedy.models.conditions.NormalField;
import com.github.silent.samurai.speedy.models.conditions.AssociatedField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class ConditionFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConditionFactory.class);

    private final EntityMetadata entityMetadata;

    public ConditionFactory(EntityMetadata entityMetadata) {
        this.entityMetadata = entityMetadata;
    }

    private BinaryCondition createCondition(QueryField field, ConditionOperator operator, Expression expression) throws SpeedyHttpException {
        return switch (operator) {
            case AND, OR -> throw new BadRequestException(
                    "AND/OR operators are not valid for a binary condition; use a boolean condition instead");
            default -> new BinaryConditionImpl(field, operator, expression);
        };
    }

    public QueryField createQueryField(String fieldName) throws SpeedyHttpException {
        String associatedField = null;
        // field name is referencing a foreign key
        if (fieldName.contains(".")) {
            String[] parts = fieldName.split("\\.");
            if (parts.length == 2) {
                fieldName = parts[0];
                associatedField = parts[1];
            } else {
                throw new BadRequestException(
                        "Field path '" + fieldName + "' has " + parts.length
                                + " segments; only single-level association paths (e.g. 'entity.field') are supported");
            }
        }
        if (associatedField != null) {
            return createAssociatedField(fieldName, associatedField);
        }
        return createNormalField(fieldName);
    }

    public QueryField createNormalField(String field) throws SpeedyHttpException {
        FieldMetadata fieldMetadata = this.entityMetadata.getField(field);
        return new NormalField(fieldMetadata);
    }

    public QueryField createAssociatedField(String field, String associatedField) throws SpeedyHttpException {
        FieldMetadata fieldMetadata = resolveAssociationOwningField(field);
        if (!fieldMetadata.isAssociation()) {
            throw new BadRequestException("field is not an association: " + fieldMetadata.getOutputPropertyName());
        }
        if (fieldMetadata.isCollection()) {
            // A to-many association can only be reached through a row-multiplying join, which would
            // duplicate result rows and corrupt pagination (and has no single FK column to join on),
            // so filtering/ordering on collection-association paths is not supported.
            throw new BadRequestException("association path is a to-many collection and cannot be used in a field path: "
                    + fieldMetadata.getOutputPropertyName());
        }
        EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
        FieldMetadata associatedFieldMetadata = associationMetadata.getField(associatedField);
        return new AssociatedField(fieldMetadata, associatedFieldMetadata);
    }

    /**
     * Resolves the owning side of an association path segment (the {@code product} in
     * {@code product.id}). Tries the field's own output name first; if that misses, falls back
     * to matching by the associated entity's name, case-insensitively — e.g. {@code product.id}
     * or {@code Product.id} both resolve to whichever field associates with the {@code Product}
     * entity, the same convention {@code $expand} already uses ({@code ExpansionPathTracker}
     * matches by entity name, not the owning field's output name — also case-insensitively).
     * Ambiguous matches (two fields to the same target entity) are rejected rather than guessed at.
     */
    private FieldMetadata resolveAssociationOwningField(String field) throws SpeedyHttpException {
        if (entityMetadata.has(field)) {
            return entityMetadata.getField(field);
        }
        List<FieldMetadata> byAssociationName = entityMetadata.getAssociatedFields().stream()
                .filter(f -> f.getAssociationMetadata().getName().equalsIgnoreCase(field))
                .collect(Collectors.toList());
        if (byAssociationName.size() == 1) {
            return byAssociationName.get(0);
        }
        if (byAssociationName.size() > 1) {
            String candidates = byAssociationName.stream()
                    .map(FieldMetadata::getOutputPropertyName)
                    .collect(Collectors.joining(", "));
            throw new BadRequestException(String.format(
                    "'%s' matches multiple associations on entity %s (%s) — reference one of these field names directly",
                    field, entityMetadata.getName(), candidates));
        }
        throw new NotFoundException(String.format("Field '%s' not found in entity %s", field, entityMetadata.getName()));
    }

    /**
     * Rejects queries that reference a sensitive field via {@code $fieldName}.
     * Only checks the resolved metadata (the target field), so a sensitive
     * field on the LEFT side of a comparison is allowed — only the RHS is blocked.
     * For FK traversals ({@code $entity.field}), the target field's sensitivity
     * is checked, not the FK owner's.
     */
    public void validateQueryFieldNotSensitive(QueryField queryField) throws BadRequestException {
        FieldMetadata metadata = queryField.getMetadataForParsing();
        if (metadata.isSensitive()) {
            LOGGER.warn("Blocked $ field reference to sensitive field '{}' on entity '{}'",
                    metadata.getOutputPropertyName(), entityMetadata.getName());
            throw new BadRequestException("Field '" + metadata.getOutputPropertyName() + "' cannot be used as a field reference");
        }
    }

    public BinaryCondition createBiCondition(QueryField normalField, ConditionOperator operator, Expression expression) throws SpeedyHttpException {
        return createCondition(normalField, operator, expression);
    }
}
