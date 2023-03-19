package com.github.silent.samurai.interfaces;

import com.github.silent.samurai.exceptions.ResourceNotFoundException;

public interface MetaModelProcessor {

    EntityMetadata findEntityMetadata(String entityName) throws ResourceNotFoundException;

    FieldMetadata findFieldMetadata(String entityName, String fieldName) throws ResourceNotFoundException;
}
