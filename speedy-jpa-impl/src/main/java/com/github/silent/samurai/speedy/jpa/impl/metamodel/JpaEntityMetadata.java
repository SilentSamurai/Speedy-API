package com.github.silent.samurai.speedy.jpa.impl.metamodel;

import com.github.silent.samurai.speedy.enums.ActionType;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.jpa.impl.interfaces.IJpaEntityMetadata;
import com.github.silent.samurai.speedy.jpa.impl.interfaces.IJpaFieldMetadata;
import com.github.silent.samurai.speedy.jpa.impl.interfaces.IJpaKeyFieldMetadata;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.metamodel.EntityType;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class JpaEntityMetadata implements IJpaEntityMetadata {

    private String name;
    private String tableName;
    private Map<String, JpaFieldMetadata> fieldMap = new HashMap<>();
    boolean hasCompositeKey;
    private EntityType<?> jpaEntityType;
    private Class<?> entityClass;
    private Class<?> keyClass;
    private ActionType actionType = ActionType.ALL;

    public void addFieldMetadata(JpaFieldMetadata metadata) {
        this.fieldMap.put(metadata.getClassFieldName(), metadata);
    }

    @Override
    public boolean has(String fieldName) {
        return fieldMap.containsKey(fieldName);
    }

    @Override
    public FieldMetadata field(String fieldName) throws NotFoundException {
        if (has(fieldName)) {
            return fieldMap.get(fieldName);
        }
        throw new NotFoundException(name + "." + fieldName);
    }

    public Set<FieldMetadata> getAllFields() {
        return fieldMap.values().stream()
                .map(FieldMetadata.class::cast)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<String> getAllFieldNames() {
        return fieldMap.values().stream()
                .map(IJpaFieldMetadata::getClassFieldName)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean hasCompositeKey() {
        return hasCompositeKey;
    }

    @Override
    public Set<KeyFieldMetadata> getKeyFields() {
        return fieldMap.values().stream()
                .filter(KeyFieldMetadata.class::isInstance)
                .map(KeyFieldMetadata.class::cast)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<String> getKeyFieldNames() {
        return fieldMap.values().stream()
                .filter(IJpaKeyFieldMetadata.class::isInstance)
                .map(IJpaKeyFieldMetadata.class::cast)
                .map(IJpaFieldMetadata::getClassFieldName)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Object createNewEntityInstance() throws Exception {
        return entityClass.getConstructor().newInstance();
    }

    @Override
    public Object createNewKeyInstance() throws Exception {
        return keyClass.getConstructor().newInstance();
    }

    @Override
    public Set<FieldMetadata> getAssociatedFields() {
        return fieldMap.values().stream()
                .filter(FieldMetadata::isAssociation)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String getDbTableName() {
        return tableName;
    }

}
