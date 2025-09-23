package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.enums.EnumMode;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyNull;

import java.util.List;

/**
 * Validates enum mode and membership.
 */
public class EnumRule implements FieldRule {

    @Override
    public void validate(FieldMetadata fm, SpeedyValue val, List<String> errors) {
        if (!fm.isEnum() || val instanceof SpeedyNull) return;

        if (fm.getOperationalEnumMode() == EnumMode.STRING) {
            if (!val.isText() && !val.isEnum()) {
                errors.add(fm.getOutputPropertyName() + " expects a string enum value");
                return;
            }
            String v = val.isEnum() ? val.asEnum() : val.asText();
            if (fm.getDynamicEnum() != null && fm.getDynamicEnum().fromName(v).isEmpty()) {
                errors.add(fm.getOutputPropertyName() + " has invalid enum value " + v);
            }
        } else {
            if (!val.isInt() && !val.isEnumOrd()) {
                errors.add(fm.getOutputPropertyName() + " expects an ordinal enum value");
                return;
            }
            long code = val.isEnumOrd() ? val.asEnumOrd() : val.asInt();
            if (fm.getDynamicEnum() != null && fm.getDynamicEnum().fromCode((int) code).isEmpty()) {
                errors.add(fm.getOutputPropertyName() + " has invalid enum value " + code);
            }
        }
    }
}
