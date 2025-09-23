package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.util.List;

/**
 * Ensures collection/scalar consistency.
 */
public class CollectionRule implements FieldRule {

    @Override
    public void validate(FieldMetadata fm, SpeedyValue value, List<String> errors) {
        if (fm.isCollection() && !value.isCollection()) {
            errors.add(fm.getOutputPropertyName() + " should be a collection");
        } else if (!fm.isCollection() && value.isCollection()) {
            errors.add(fm.getOutputPropertyName() + " should not be a collection");
        }
    }
}
