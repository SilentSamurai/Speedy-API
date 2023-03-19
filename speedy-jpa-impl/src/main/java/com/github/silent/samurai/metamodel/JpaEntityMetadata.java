package com.github.silent.samurai.metamodel;

import com.github.silent.samurai.exceptions.ResourceNotFoundException;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.FieldMetadata;
import lombok.Data;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import javax.persistence.metamodel.EntityType;
import java.util.*;

@Data
public class JpaEntityMetadata implements EntityMetadata {

    private String name;
    private String tableName;
    private Set<FieldMetadata> allFields = new HashSet<>();
    private Map<String, JpaFieldMetadata> fieldMap = new HashMap<>();
    private Set<String> keyFields = new HashSet<>();
    private EntityType<?> jpaEntityType;
    private Class<?> entityClass;
    private Class<?> keyClass;

    @Override
    public boolean has(String fieldName) {
        return fieldMap.containsKey(fieldName);
    }

    @Override
    public FieldMetadata field(String fieldName) throws ResourceNotFoundException {
        if (has(fieldName)) {
            return fieldMap.get(fieldName);
        }
        throw new ResourceNotFoundException(name + "." + fieldName);
    }

    public Set<FieldMetadata> getAllFields() {
        return allFields;
    }

    @Override
    public Object createNewEntityInstance() throws Exception {
        return entityClass.getConstructor().newInstance();
    }

    @Override
    public Object createNewKeyInstance() throws Exception {
        return keyClass.getConstructor().newInstance();
    }
}
