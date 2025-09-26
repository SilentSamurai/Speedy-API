package com.github.silent.samurai.speedy.annotations.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** Marks a numeric field that must be positive or zero (>=0). */
@Target(FIELD)
@Retention(RUNTIME)
public @interface SpeedyPositiveOrZero {
}
