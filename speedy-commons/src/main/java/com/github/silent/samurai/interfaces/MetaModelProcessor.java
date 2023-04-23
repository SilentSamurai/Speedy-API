package com.github.silent.samurai.interfaces;

import com.github.silent.samurai.exceptions.NotFoundException;

import java.util.Collection;

public interface MetaModelProcessor {

    Collection<EntityMetadata> getAllEntityMetadata();

    boolean hasEntityMetadata(Class<?> entityType);

    EntityMetadata findEntityMetadata(Class<?> entityType) throws NotFoundException;

    boolean hasEntityMetadata(String entityName);

    EntityMetadata findEntityMetadata(String entityName) throws NotFoundException;

    FieldMetadata findFieldMetadata(String entityName, String fieldName) throws NotFoundException;
}
