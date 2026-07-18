package com.github.silent.samurai.speedy.annotations.validation;

import com.github.silent.samurai.speedy.enums.SpeedyDateFormat;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Ensures that a date string conforms to a specific ISO format.
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface SpeedyDateWithFormat {
    SpeedyDateFormat iso() default SpeedyDateFormat.NONE;
}
