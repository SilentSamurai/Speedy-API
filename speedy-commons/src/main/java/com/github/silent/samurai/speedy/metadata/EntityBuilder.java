package com.github.silent.samurai.speedy.metadata;

import com.github.silent.samurai.speedy.enums.ActionType;
import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public class EntityBuilder {
    boolean hasCompositeKey;
    Set<ActionType> actionTypes = new HashSet<>(List.of(ActionType.ALL));
    Map<String, FieldBuilder> fieldMap = new HashMap<>();
    private String name;
    private String dbTableName;

    public Iterable<FieldBuilder> fields() {
        return fieldMap.values();
    }

    public Iterable<KeyFieldBuilder> keyFields() {
        return fieldMap.values().stream()
                .filter(KeyFieldBuilder.class::isInstance)
                .map(KeyFieldBuilder.class::cast)
                .collect(Collectors.toList());
    }

    public EntityBuilder name(String name) {
        this.name = name;
        return this;
    }

    public EntityBuilder dbTableName(String dbTableName) {
        Objects.requireNonNull(dbTableName);
        this.dbTableName = dbTableName;
        return this;
    }

    public EntityBuilder hasCompositeKey(boolean hasCompositeKey) {
        this.hasCompositeKey = hasCompositeKey;
        return this;
    }

    public EntityBuilder addActionType(ActionType actionType) {
        this.actionTypes.add(actionType);
        return this;
    }

    public FieldBuilder ref(String name) throws NotFoundException {
        if (fieldMap.containsKey(name)) {
            return fieldMap.get(name);
        }
        throw new NotFoundException(String.format("Field '%s' not found in entity %s", name, this.name));
    }

    public FieldBuilder field(String fieldName) {
        assert !fieldName.isBlank();
        FieldBuilder fieldBuilder = new FieldBuilder(this, fieldName);
        this.fieldMap.put(fieldName, fieldBuilder);
        return fieldBuilder;
    }

    // Overload to support specifying output property name, DB column name and column type
    public FieldBuilder field(String outputPropertyName, String dbColumnName, ColumnType columnType) {
        Objects.requireNonNull(outputPropertyName);
        Objects.requireNonNull(dbColumnName);
        Objects.requireNonNull(columnType);
        FieldBuilder fieldBuilder = new FieldBuilder(this, outputPropertyName)
                .dbColumnName(dbColumnName)
                .columnType(columnType);
        this.fieldMap.put(outputPropertyName, fieldBuilder);
        return fieldBuilder;
    }

    public KeyFieldBuilder keyField(String fieldName) {
        assert !fieldName.isBlank();
        KeyFieldBuilder fieldBuilder = new KeyFieldBuilder(this, fieldName);
        this.fieldMap.put(fieldName, fieldBuilder);
        return fieldBuilder;
    }

    // Overload to support specifying output property name, DB column name and column type for key fields
    public KeyFieldBuilder keyField(String outputPropertyName, String dbColumnName, ColumnType columnType) {
        Objects.requireNonNull(outputPropertyName);
        Objects.requireNonNull(dbColumnName);
        Objects.requireNonNull(columnType);
        KeyFieldBuilder fieldBuilder = new KeyFieldBuilder(this, outputPropertyName);
        fieldBuilder.dbColumnName(dbColumnName)
                .columnType(columnType);
        this.fieldMap.put(outputPropertyName, fieldBuilder);
        return fieldBuilder;
    }


    public EntityMetadataImpl build() throws NotFoundException {

        Map<String, FieldMetadata> fieldMetadataMap = new HashMap<>();
        for (Map.Entry<String, FieldBuilder> e : fieldMap.entrySet()) {
            Map.Entry<String, FieldMetadataImpl> entry = Map.entry(e.getKey(), e.getValue().build());
            if (fieldMetadataMap.put(entry.getKey(), entry.getValue()) != null) {
                throw new IllegalStateException("Duplicate key");
            }
        }

        EntityMetadataImpl entityMetadata = new EntityMetadataImpl(name, dbTableName, hasCompositeKey, actionTypes, fieldMetadataMap);

        for (FieldMetadata fieldMetadata : fieldMetadataMap.values()) {
            FieldMetadataImpl fieldMetadataImpl = (FieldMetadataImpl) fieldMetadata;
            fieldMetadataImpl.setEntityMetadata(entityMetadata);
        }

        return entityMetadata;
    }
}
