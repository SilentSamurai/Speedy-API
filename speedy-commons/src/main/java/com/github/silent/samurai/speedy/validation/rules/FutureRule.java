package com.github.silent.samurai.speedy.validation.rules;

import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Validates that the supplied date/datetime/time/zoned-datetime is strictly in the future.
 */
public class FutureRule implements FieldRule {
    private final String message;
    private final Clock clock;

    public FutureRule(String msg) {
        this(msg, Clock.systemDefaultZone());
    }

    public FutureRule(String msg, Clock clock) {
        this.message = msg == null || msg.isBlank() ? "must be in the future" : msg;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Override
    public void validate(FieldMetadata fm, SpeedyValue val, List<String> errors) {
        if (val == null || val.isEmpty()) return;
        if (!val.isTemporal()) return; // only temporal values

        boolean valid;
        if (val.isDate()) {
            valid = val.asDate().isAfter(LocalDate.now(clock));
        } else if (val.isDateTime()) {
            valid = val.asDateTime().isAfter(LocalDateTime.now(clock));
        } else if (val.isTime()) {
            valid = val.asTime().isAfter(LocalTime.now(clock));
        } else if (val.isZonedDateTime()) {
            valid = val.asZonedDateTime().isAfter(ZonedDateTime.now(clock));
        } else {
            return;
        }
        if (!valid) {
            errors.add(fm.getOutputPropertyName() + " " + message);
        }
    }
}
