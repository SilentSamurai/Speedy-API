package com.github.silent.samurai.annotations;

import com.github.silent.samurai.interfaces.ISpeedyCustomValidation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SpeedyCustomValidation {

    Class<ISpeedyCustomValidation> value() default ISpeedyCustomValidation.class;
}
