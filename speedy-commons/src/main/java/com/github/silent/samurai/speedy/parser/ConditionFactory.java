package com.github.silent.samurai.speedy.parser;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.Expression;
import com.github.silent.samurai.speedy.interfaces.query.QueryField;
import com.github.silent.samurai.speedy.models.conditions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConditionFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConditionFactory.class);

    private final EntityMetadata entityMetadata;

    public ConditionFactory(EntityMetadata entityMetadata) {
        this.entityMetadata = entityMetadata;
    }

    private BinaryCondition createCondition(QueryField field, ConditionOperator operator, Expression expression) throws SpeedyHttpException {
        return switch (operator) {
            case EQ -> new EqCondition(field, expression);
            case NEQ -> new NotEqCondition(field, expression);
            case LT -> new LessThanCondition(field, expression);
            case GT -> new GreaterThanCondition(field, expression);
            case LTE -> new LessThanEqualCondition(field, expression);
            case GTE -> new GreaterThanEqualCondition(field, expression);
            case IN -> new InCondition(field, expression);
            case NOT_IN -> new NotInCondition(field, expression);
            case PATTERN_MATCHING -> new MatchingCondition(field, expression);
            case BETWEEN -> new BetweenCondition(field, expression);
            case ISNULL -> new IsNullCondition(field, expression);
            case ISNOTNULL -> new IsNotNullCondition(field, expression);
            case AND, OR -> throw new BadRequestException("");
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
        FieldMetadata fieldMetadata = this.entityMetadata.getField(field);
        if (!fieldMetadata.isAssociation()) {
            throw new BadRequestException("field is not an association: " + fieldMetadata.getOutputPropertyName());
        }
        EntityMetadata associationMetadata = fieldMetadata.getAssociationMetadata();
        FieldMetadata associatedFieldMetadata = associationMetadata.getField(associatedField);
        return new AssociatedField(fieldMetadata, associatedFieldMetadata);
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
