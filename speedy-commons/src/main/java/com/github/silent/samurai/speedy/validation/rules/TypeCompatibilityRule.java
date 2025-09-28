package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyNull;

import java.util.List;

/**
 * Ensures non-enum scalar values match the expected ValueType declared in metadata.
 */
public class TypeCompatibilityRule implements FieldRule {

    @Override
    public void validate(FieldMetadata fm, SpeedyValue val, List<String> errors) {
        if (fm.isEnum() || fm.isAssociation() || fm.isCollection() || val instanceof SpeedyNull) return;

        ValueType expected = fm.getValueType();
        if (expected == ValueType.OBJECT || expected == ValueType.COLLECTION) return;

        if (val.getValueType() != expected) {
            errors.add(fm.getOutputPropertyName() + " expects type " + expected + " but got " + val.getValueType());
        }
    }
}
