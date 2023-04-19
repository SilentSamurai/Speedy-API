package com.github.silent.samurai.annotations;

import com.github.silent.samurai.enums.SpeedyRequestType;
import com.github.silent.samurai.interfaces.ISpeedyCustomValidation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SpeedyValidator {

    Class<?> value() default ISpeedyCustomValidation.class;

    SpeedyRequestType[] requests() default {SpeedyRequestType.CREATE, SpeedyRequestType.UPDATE};

}
