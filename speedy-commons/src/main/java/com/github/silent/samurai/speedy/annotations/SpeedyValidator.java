package com.github.silent.samurai.speedy.annotations;

import com.github.silent.samurai.speedy.enums.SpeedyValidationRequestType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SpeedyValidator {

    String entity() default "";

    SpeedyValidationRequestType[] requests() default {SpeedyValidationRequestType.CREATE, SpeedyValidationRequestType.UPDATE};

    /**
     * By default a custom validator runs <em>in addition</em> to the built-in
     * {@code DefaultFieldValidator} (required-field / type / association / collection
     * / enum checks): the default checks run first, then this validator.
     * <p>
     * Set to {@code true} to take full ownership of validation for the annotated
     * entity and request type(s): the built-in checks are skipped and only this
     * validator runs.
     */
    boolean replacesDefault() default false;

}
