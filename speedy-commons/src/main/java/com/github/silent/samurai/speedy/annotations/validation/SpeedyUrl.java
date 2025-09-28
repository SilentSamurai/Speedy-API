package com.github.silent.samurai.speedy.annotations.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** Marks a field that must contain a valid URL string. */
@Target(FIELD)
@Retention(RUNTIME)
public @interface SpeedyUrl {
}
