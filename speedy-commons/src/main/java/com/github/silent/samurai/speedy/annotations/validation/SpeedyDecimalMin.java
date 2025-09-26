package com.github.silent.samurai.speedy.annotations.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Minimum decimal value constraint.
 * Similar to {@code jakarta.validation.constraints.DecimalMin}.
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface SpeedyDecimalMin {
    /** The minimum value expressed as a string to preserve precision. */
    String value();

    /** Whether the value is inclusive ("valid >= min") or exclusive ("> min"). Default true (inclusive). */
    boolean inclusive() default true;
}
