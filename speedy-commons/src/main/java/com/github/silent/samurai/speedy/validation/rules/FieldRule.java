package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.util.List;

/**
 * A single, composable validation unit for a field.
 * Implementations should be <b>stateless</b> and thread-safe.
 */
public interface FieldRule {

    /**
     * Validate the supplied value for the given field.
     *
     * @param fm     field metadata describing constraints
     * @param value  the {@link SpeedyValue} to validate (never null)
     * @param errors mutable list to add error messages – DO NOT throw; accumulate
     */
    void validate(FieldMetadata fm, SpeedyValue value, List<String> errors);
}
