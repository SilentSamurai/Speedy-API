package com.github.silent.samurai.interfaces;

import com.github.silent.samurai.exceptions.ResourceNotFoundException;

import java.util.Set;

public interface EntityMetadata {

    String getName();

    boolean has(String fieldName);

    FieldMetadata field(String fieldName) throws ResourceNotFoundException;

    Set<FieldMetadata> getAllFields();

    Class<?> getEntityClass();

    Class<?> getKeyClass();

    Set<String> getKeyFields();

    Object createNewEntityInstance() throws Exception;

    Object createNewKeyInstance() throws Exception;

}
