package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyNull;

import java.util.List;
import java.util.Objects;

/**
 * Validates association fields: value must be nested object and must contain non-generated key fields.
 */
public class AssociationRule implements FieldRule {

    @Override
    public void validate(FieldMetadata fm, SpeedyValue value, List<String> errors) {
        if (!fm.isAssociation() || value instanceof SpeedyNull) return;

        // Value must be object (no scalar FK allowed per latest spec)
        if (!value.isObject()) {
            errors.add(fm.getOutputPropertyName() + " should be an associated object");
            return;
        }

        SpeedyEntity nested = value.asObject();
        EntityMetadata assocMeta = fm.getAssociationMetadata();
        if (assocMeta == null) return; // nothing to validate

        for (KeyFieldMetadata keyField : assocMeta.getKeyFields()) {
            if (keyField.shouldGenerateKey()) continue;
            if (!nested.has(keyField)) {
                errors.add(fm.getOutputPropertyName() + "." + keyField.getOutputPropertyName() + " is required");
                continue;
            }
            SpeedyValue kv = nested.get(keyField);
            if (kv == null || kv instanceof SpeedyNull || kv.isEmpty()) {
                errors.add(fm.getOutputPropertyName() + "." + keyField.getOutputPropertyName() + " cannot be null or empty");
            }
        }
    }
}
