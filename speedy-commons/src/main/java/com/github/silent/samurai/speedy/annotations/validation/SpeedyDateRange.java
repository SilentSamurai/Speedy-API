package com.github.silent.samurai.speedy.annotations.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** Ensures the date is within a specific inclusive range */
@Target(FIELD)
@Retention(RUNTIME)
public @interface SpeedyDateRange {
    String min();
    String max();
    String message() default "Date out of range";
}
