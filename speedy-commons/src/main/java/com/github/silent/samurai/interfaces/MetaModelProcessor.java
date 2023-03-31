package com.github.silent.samurai.interfaces;

import com.github.silent.samurai.exceptions.ResourceNotFoundException;

import java.util.Collection;

public interface MetaModelProcessor {

    Collection<EntityMetadata> getAllEntityMetadata();

    EntityMetadata findEntityMetadata(String entityName) throws ResourceNotFoundException;

    FieldMetadata findFieldMetadata(String entityName, String fieldName) throws ResourceNotFoundException;
}
