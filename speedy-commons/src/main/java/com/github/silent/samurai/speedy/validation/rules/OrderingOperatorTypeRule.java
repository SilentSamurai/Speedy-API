package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.util.List;

/**
 * Restricts the relational operators ({@code <}, {@code <=}, {@code >}, {@code >=},
 * {@code $between}) to field types that have a meaningful ordering: numbers,
 * temporal types, text (lexicographic) and ordinal enums. Booleans, string enums
 * and container types are rejected.
 */
public class OrderingOperatorTypeRule implements QueryRule {

    @Override
    public void validate(FieldMetadata field, ConditionOperator operator, SpeedyValue literal, List<String> errors) {
        if (!isRelational(operator)) {
            return;
        }
        if (!isOrderable(field.getValueType())) {
            errors.add("operator " + operator + " is not supported on field '"
                    + field.getOutputPropertyName() + "' of type " + field.getValueType());
        }
    }

    private static boolean isRelational(ConditionOperator operator) {
        return switch (operator) {
            case LT, LTE, GT, GTE, BETWEEN -> true;
            default -> false;
        };
    }

    private static boolean isOrderable(ValueType type) {
        return switch (type) {
            case INT, FLOAT, ENUM_ORD, TEXT, DATE, TIME, DATE_TIME, ZONED_DATE_TIME -> true;
            case BOOL, ENUM, OBJECT, COLLECTION, NULL -> false;
        };
    }
}
