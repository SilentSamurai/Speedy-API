package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Validates that a date is between min and max (inclusive). Works on LocalDate values. */
public class DateRangeRule implements FieldRule {
    private final LocalDate min;
    private final LocalDate max;
    private final String message;

    public DateRangeRule(String min, String max, String message) {
        this.min = LocalDate.parse(min);
        this.max = LocalDate.parse(max);
        this.message = message == null || message.isBlank() ? "date out of range" : message;
    }

    @Override
    public void validate(FieldMetadata fm, SpeedyValue val, List<String> errors) {
        if (val == null || val.isEmpty()) return;
        if (!val.isDate()) return;
        LocalDate d = val.asDate();
        if (d.isBefore(min) || d.isAfter(max)) {
            errors.add(fm.getOutputPropertyName() + " " + message);
        }
    }
}
