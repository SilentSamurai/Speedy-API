package com.github.silent.samurai.speedy.annotations;


import com.github.silent.samurai.speedy.enums.SpeedyEventType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SpeedyEvent {

    String value() default "";

    SpeedyEventType[] eventType();

}
