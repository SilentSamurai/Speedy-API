package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.util.List;

/**
 * Ensures that the value is a non-blank string (i.e. not null/empty and contains at least one non-whitespace character).
 * <p>
 * Added to support {@code jakarta.validation.constraints.NotBlank}.
 */
public class NotBlankRule implements FieldRule {

    @Override
    public void validate(FieldMetadata fm, SpeedyValue val, List<String> errors) {
        if (val == null || val.isEmpty()) return; // null / empty handled elsewhere (required rule)
        if (!val.isText() || val.asText().trim().isEmpty()) {
            errors.add(fm.getOutputPropertyName() + " cannot be blank");
        }
    }
}
