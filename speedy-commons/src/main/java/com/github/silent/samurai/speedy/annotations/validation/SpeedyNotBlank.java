package com.github.silent.samurai.speedy.annotations.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a string field that must not be blank (non-null, non-empty, and contains a non-whitespace character).
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface SpeedyNotBlank {
}
