package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Validates that the supplied date/datetime is strictly in the future. */
public class FutureRule implements FieldRule {
    private final String message;
    public FutureRule(String msg) { this.message = msg == null || msg.isBlank() ? "must be in the future" : msg; }

    @Override
    public void validate(FieldMetadata fm, SpeedyValue val, List<String> errors) {
        if (val == null || val.isEmpty()) return;
        if (!val.isTemporal()) return; // only temporal values

        boolean valid = true;
        if (val.isDate()) {
            LocalDate d = val.asDate();
            valid = d.isAfter(LocalDate.now());
        } else if (val.isDateTime()) {
            LocalDateTime dt = val.asDateTime();
            valid = dt.isAfter(LocalDateTime.now());
        }
        if (!valid) {
            errors.add(fm.getOutputPropertyName() + " " + message);
        }
    }
}
