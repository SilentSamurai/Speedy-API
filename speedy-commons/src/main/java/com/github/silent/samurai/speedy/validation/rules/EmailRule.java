package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates that a string value is a well-formed email when the field is annotated with @SpeedyEmail.
 */
public class EmailRule implements FieldRule {
    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9][A-Za-z0-9.-]*[A-Za-z0-9]\\.[A-Za-z]{2,}$");

    @Override
    public void validate(FieldMetadata fm, SpeedyValue val, List<String> errors) {
        if (val == null || val.isEmpty()) return;

        if (!val.isText() || !EMAIL.matcher(val.asText()).matches()) {
            errors.add(fm.getOutputPropertyName() + " must be a valid email");
        }
    }
}
