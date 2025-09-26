package com.github.silent.samurai.speedy.annotations.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Maximum decimal value constraint.
 * Similar to {@code jakarta.validation.constraints.DecimalMax}.
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface SpeedyDecimalMax {
    String value();
    boolean inclusive() default true;
}
