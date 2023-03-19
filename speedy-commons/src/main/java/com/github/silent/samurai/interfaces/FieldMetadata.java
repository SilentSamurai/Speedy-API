package com.github.silent.samurai.interfaces;

import com.github.silent.samurai.enums.IgnoreType;

import java.lang.reflect.InvocationTargetException;

public interface FieldMetadata {

    Object extractFieldValue(Object extity) throws IllegalAccessException, InvocationTargetException;

    boolean isAssociation();

    boolean isCollection();

    String getFieldName();

    boolean isKeyField();

    IgnoreType getIgnoreType();

}
