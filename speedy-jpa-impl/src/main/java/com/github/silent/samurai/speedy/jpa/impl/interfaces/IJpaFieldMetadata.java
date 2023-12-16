package com.github.silent.samurai.speedy.jpa.impl.interfaces;

import com.github.silent.samurai.speedy.interfaces.FieldMetadata;

public interface IJpaFieldMetadata extends FieldMetadata {

    Object getEntityFieldValue(Object entity);

    boolean setEntityFieldWithValue(Object entity, Object value);

    String getClassFieldName();
}
