package com.github.silent.samurai.speedy.jpa.impl.interfaces;

import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;

public interface IJpaKeyFieldMetadata extends KeyFieldMetadata, IJpaFieldMetadata {

    boolean setIdFieldWithValue(Object idInstance, Object value);

    Object getIdFieldValue(Object idInstance);

}
