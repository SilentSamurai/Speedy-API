package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyNull;

import java.util.List;

/**
 * Ensures that object values are allowed only for association fields.
 * If a field is *not* marked as association yet a nested object value is supplied,
 * this rule registers an error.
 */
public class NonAssociationObjectRule implements FieldRule {
    @Override
    public void validate(FieldMetadata fm, SpeedyValue val, List<String> errors) {
        if (val == null || val instanceof SpeedyNull) return;

        if (!fm.isAssociation() && val.isObject()) {
            errors.add(fm.getOutputPropertyName() + " is not an association field");
        }
    }
}
