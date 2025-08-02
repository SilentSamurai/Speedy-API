package com.github.silent.samurai.speedy.metadata;

import com.github.silent.samurai.speedy.enums.ActionType;
import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import lombok.Getter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class EntityBuilder {
    private String name;
    private String dbTableName;
    boolean hasCompositeKey;
    Set<ActionType> actionTypes = new HashSet<>(List.of(ActionType.ALL));
    Map<String, FieldBuilder> fieldMap = new HashMap<>();

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

    public FieldBuilder field(String fieldName, String columnName, ColumnType columnType) {
        assert !fieldName.isBlank();
        assert !columnName.isBlank();
        FieldBuilder fieldBuilder = new FieldBuilder(this, columnName, columnType, fieldName);
        this.fieldMap.put(fieldName, fieldBuilder);
        return fieldBuilder;
    }

    public KeyFieldBuilder keyField(String fieldName, String columnName, ColumnType columnType) {
        assert !fieldName.isBlank();
        assert !columnName.isBlank();
        KeyFieldBuilder fieldBuilder = new KeyFieldBuilder(this, columnName, columnType, fieldName);
        this.fieldMap.put(fieldName, fieldBuilder);
        return fieldBuilder;
    }


    public EntityMetadataImpl build() {

        Map<String, FieldMetadata> fieldMetadataMap = fieldMap.entrySet()
                .stream()
                .map(e -> Map.entry(e.getKey(), e.getValue().build()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        EntityMetadataImpl entityMetadata = new EntityMetadataImpl(name, dbTableName, hasCompositeKey, actionTypes, fieldMetadataMap);

        for (FieldMetadata fieldMetadata : fieldMetadataMap.values()) {
            FieldMetadataImpl fieldMetadataImpl = (FieldMetadataImpl) fieldMetadata;
            fieldMetadataImpl.setEntityMetadata(entityMetadata);
        }

        return entityMetadata;
    }
}
