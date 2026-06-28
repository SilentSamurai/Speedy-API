package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.util.List;

/**
 * Restricts the {@code PATTERN_MATCHING} ({@code $matches} / SQL {@code LIKE})
 * operator to text fields. Applying a wildcard match to a numeric, boolean or
 * temporal column is meaningless and almost always a client bug.
 * <p>
 * This is the backend-agnostic home for the constraint; query-processor
 * implementations must not re-validate it. The message intentionally matches the
 * historical wording so existing API contracts/tests stay stable.
 */
public class PatternMatchingTypeRule implements QueryRule {

    @Override
    public void validate(FieldMetadata field, ConditionOperator operator, SpeedyValue literal, List<String> errors) {
        if (operator == ConditionOperator.PATTERN_MATCHING && field.getValueType() != ValueType.TEXT) {
            errors.add("only text values are supported for $matches (field '"
                    + field.getOutputPropertyName() + "' is " + field.getValueType() + ")");
        }
    }
}
