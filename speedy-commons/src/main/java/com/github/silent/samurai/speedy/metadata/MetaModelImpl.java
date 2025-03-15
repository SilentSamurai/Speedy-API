package com.github.silent.samurai.speedy.metadata;

import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MetaModelImpl implements MetaModel {

    private final Map<String, EntityMetadataImpl> entityMap = new HashMap<>();

    public void add(EntityMetadataImpl entity) {
        entityMap.put(entity.getName(), entity);
    }

    @Override
    public Collection<EntityMetadata> getAllEntityMetadata() {
        return entityMap.values().stream().map(em -> (EntityMetadata) em).toList();
    }

    @Override
    public boolean hasEntityMetadata(String entityName) {
        return entityMap.containsKey(entityName);
    }

    @Override
    public EntityMetadata findEntityMetadata(String entityName) throws NotFoundException {
        if (hasEntityMetadata(entityName)) {
            return entityMap.get(entityName);
        }
        throw new NotFoundException("Entity " + entityName + " not found");
    }

    @Override
    public FieldMetadata findFieldMetadata(String entityName, String fieldName) throws NotFoundException {
        EntityMetadata entityMetadata = findEntityMetadata(entityName);
        return entityMetadata.field(fieldName);
    }
}
