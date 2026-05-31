package com.github.silent.samurai.speedy.annotations;

import com.github.silent.samurai.speedy.enums.ColumnType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SpeedyType {

    ColumnType value();
}
