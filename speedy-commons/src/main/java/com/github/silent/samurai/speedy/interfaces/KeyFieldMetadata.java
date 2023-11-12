package com.github.silent.samurai.speedy.interfaces;


public interface KeyFieldMetadata extends FieldMetadata {


    boolean isKeyField();

    boolean setIdFieldWithValue(Object idInstance, Object value);

    Object getIdFieldValue(Object idInstance);

}
