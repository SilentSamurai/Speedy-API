package com.github.silent.samurai.speedy.annotations;

import com.github.silent.samurai.speedy.enums.ActionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SpeedyAction {

    ActionType value() default ActionType.ALL;
}
