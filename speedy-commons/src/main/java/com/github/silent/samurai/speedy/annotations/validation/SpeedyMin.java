package com.github.silent.samurai.speedy.annotations.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** Minimum numeric value constraint. */
@Target(FIELD)
@Retention(RUNTIME)
public @interface SpeedyMin {
    long value();
}
