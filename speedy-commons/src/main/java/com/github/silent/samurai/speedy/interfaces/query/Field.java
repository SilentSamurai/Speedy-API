package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.interfaces.FieldMetadata;

public interface Field {

    FieldMetadata getFieldMetadata();

    boolean hasAssociatedFieldMetadata();

    FieldMetadata getAssociatedFieldMetadata();
}
