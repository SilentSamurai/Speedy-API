package com.github.silent.samurai.speedy.annotations;

import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.interfaces.ISpeedyCustomValidation;

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
