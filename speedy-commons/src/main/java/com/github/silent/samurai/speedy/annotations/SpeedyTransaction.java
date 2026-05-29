package com.github.silent.samurai.speedy.annotations;

import com.github.silent.samurai.speedy.enums.TransactionMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SpeedyTransaction {
    TransactionMode value() default TransactionMode.PER_ENTITY;
}
