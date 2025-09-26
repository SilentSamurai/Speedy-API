package com.github.silent.samurai.speedy.annotations.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** String length constraint. */
@Target(FIELD)
@Retention(RUNTIME)
public @interface SpeedyLength {
    int min() default 0;
    int max();
}
