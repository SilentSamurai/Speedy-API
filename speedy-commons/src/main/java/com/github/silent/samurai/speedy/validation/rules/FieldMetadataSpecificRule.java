package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.util.List;

/**
 * Executes parameterised constraints listed in {@link FieldMetadata#getValidations()}.
 */
public class FieldMetadataSpecificRule implements FieldRule {

    @Override
    public void validate(FieldMetadata fm, SpeedyValue val, List<String> errors) {
        if (val == null || val.isEmpty()) return;

        for (FieldRule rule : fm.getValidations()) {
            rule.validate(fm, val, errors);
        }
    }
}
