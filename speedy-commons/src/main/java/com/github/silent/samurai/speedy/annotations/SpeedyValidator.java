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

}
