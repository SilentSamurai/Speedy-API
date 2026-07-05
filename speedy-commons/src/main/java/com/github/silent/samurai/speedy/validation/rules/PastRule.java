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
 * Validates that the supplied date/datetime/time/zoned-datetime is strictly in the past.
 */
public class PastRule implements FieldRule {
    private final String message;
    private final Clock clock;

    public PastRule(String msg) {
        this(msg, Clock.systemDefaultZone());
    }

    public PastRule(String msg, Clock clock) {
        this.message = msg == null || msg.isBlank() ? "must be in the past" : msg;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Override
    public void validate(FieldMetadata fm, SpeedyValue val, List<String> errors) {
        if (val == null || val.isEmpty()) return;
        if (!val.isTemporal()) return;

        boolean valid;
        if (val.isDate()) {
            valid = val.asDate().isBefore(LocalDate.now(clock));
        } else if (val.isDateTime()) {
            valid = val.asDateTime().isBefore(LocalDateTime.now(clock));
        } else if (val.isTime()) {
            valid = val.asTime().isBefore(LocalTime.now(clock));
        } else if (val.isZonedDateTime()) {
            valid = val.asZonedDateTime().isBefore(ZonedDateTime.now(clock));
        } else {
            return;
        }
        if (!valid) {
            errors.add(fm.getOutputPropertyName() + " " + message);
        }
    }
}
