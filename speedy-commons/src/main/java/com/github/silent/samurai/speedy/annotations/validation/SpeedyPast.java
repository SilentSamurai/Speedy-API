package com.github.silent.samurai.speedy.annotations.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** Ensures the date is in the past */
@Target(FIELD)
@Retention(RUNTIME)
public @interface SpeedyPast {
    String message() default "Date must be in the past";
}
