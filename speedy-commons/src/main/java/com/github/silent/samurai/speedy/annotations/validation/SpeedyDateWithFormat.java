package com.github.silent.samurai.speedy.annotations.validation;

import org.springframework.format.annotation.DateTimeFormat;

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
    DateTimeFormat.ISO iso() default DateTimeFormat.ISO.NONE;
}
