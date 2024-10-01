package com.github.silent.samurai.speedy.jpa.impl.data;

import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.jpa.impl.interfaces.IJpaEntityMetadata;
import com.github.silent.samurai.speedy.jpa.impl.interfaces.IJpaFieldMetadata;
import lombok.Data;
import lombok.SneakyThrows;

import javax.persistence.Id;
import javax.persistence.IdClass;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class StaticEntityMetadata implements IJpaEntityMetadata {

    private Class<?> entityClass;
    private Class<?> keyClass;

    public static EntityMetadata createEntityMetadata(Class<?> entityClass) {
        StaticEntityMetadata entityMetadata = new StaticEntityMetadata();
        entityMetadata.setEntityClass(entityClass);
        IdClass annotation = entityClass.getAnnotation(IdClass.class);
        if (annotation != null) {
            entityMetadata.setKeyClass(annotation.value());
        } else {
            Optional<Field> idField = Arrays.stream(entityClass.getDeclaredFields())
                    .filter(field -> field.getAnnotation(Id.class) != null)
                    .filter(field -> !field.getName().startsWith("__"))
                    .findAny();
            entityMetadata.setKeyClass(idField.get().getType());
        }
        return entityMetadata;
    }

    @Override
    public String getName() {
        return entityClass.getSimpleName();
    }

    @Override
    public boolean has(String fieldName) {
        try {
            entityClass.getDeclaredField(fieldName);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    @SneakyThrows
    @Override
    public FieldMetadata field(String fieldName) throws NotFoundException {
        Field declaredField = entityClass.getDeclaredField(fieldName);
        return StaticFieldMetadata.createFieldMetadata(declaredField);
    }

    @Override
    public Set<FieldMetadata> getAllFields() {
        return Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> !field.getName().startsWith("__"))
                .map(StaticFieldMetadata::createFieldMetadata)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getAllFieldNames() {
        return getAllFields().stream()
                .map(IJpaFieldMetadata.class::cast)
                .map(IJpaFieldMetadata::getClassFieldName)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean hasCompositeKey() {
        return entityClass.getAnnotation(IdClass.class) != null;
    }

    @Override
    public Class<?> getEntityClass() {
        return entityClass;
    }

    @Override
    public Class<?> getKeyClass() {
        return keyClass;
    }

    @Override
    public Set<KeyFieldMetadata> getKeyFields() {
        if (this.hasCompositeKey()) {
            return Arrays.stream(keyClass.getDeclaredFields())
                    .filter(field -> !field.getName().startsWith("__"))
                    .map(StaticFieldMetadata::createFieldMetadata)
                    .collect(Collectors.toSet());
        }
        return Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> !field.getName().startsWith("__"))
                .filter(field -> field.getAnnotation(Id.class) != null)
                .map(StaticFieldMetadata::createFieldMetadata)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getKeyFieldNames() {
        return getKeyFields().stream()
                .map(IJpaFieldMetadata.class::cast)
                .map(IJpaFieldMetadata::getClassFieldName)
                .collect(Collectors.toSet());
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
        return null;
    }

    @Override
    public Optional<FieldMetadata> getAssociatedField(EntityMetadata secondaryResource) {
        return Optional.empty();
    }

    @Override
    public String getDbTableName() {
        return "";
    }
}
