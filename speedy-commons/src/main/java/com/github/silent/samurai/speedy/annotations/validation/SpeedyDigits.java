package com.github.silent.samurai.speedy.annotations.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Constraint for the number of integer and fractional digits of a numeric value.
 * Mirrors {@code jakarta.validation.constraints.Digits}.
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface SpeedyDigits {
    int integer();
    int fraction();
}
