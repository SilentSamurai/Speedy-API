package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.List;

/** Ensures that a date/datetime value matches the desired ISO style (DATE or DATE_TIME).*/
public class DateFormatRule implements FieldRule {
    private final DateTimeFormat.ISO iso;
    public DateFormatRule(DateTimeFormat.ISO iso) { this.iso = iso; }

    @Override
    public void validate(FieldMetadata fm, SpeedyValue val, List<String> errors) {
        if (val == null || val.isEmpty()) return;
        switch (iso) {
            case DATE -> {
                if (!val.isDate()) {
                    errors.add(fm.getOutputPropertyName() + " must be an ISO DATE");
                }
            }
            case DATE_TIME -> {
                if (!val.isDateTime()) {
                    errors.add(fm.getOutputPropertyName() + " must be an ISO DATE_TIME");
                }
            }
            default -> {
                // if ISO.NONE or others -> do nothing
            }
        }
    }
}
