package com.github.silent.samurai.speedy.processors;

import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.metamodel.JpaEntityMetadata;
import com.github.silent.samurai.speedy.query.JpaQueryProcessorImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.EntityType;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class JpaMetaModelProcessor implements MetaModelProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaMetaModelProcessor.class);

    private final Map<String, JpaEntityMetadata> entityMap = new HashMap<>();
    private final Map<Class<?>, JpaEntityMetadata> typeMap = new HashMap<>();

    public JpaMetaModelProcessor(EntityManagerFactory entityManagerFactory) {
        Set<EntityType<?>> entities = entityManagerFactory.getMetamodel().getEntities();
        for (EntityType<?> entityType : entities) {
            JpaEntityMetadata entityMetadata = JpaEntityProcessor.processEntity(entityType);
            entityMap.put(entityType.getName(), entityMetadata);
            typeMap.put(entityMetadata.getEntityClass(), entityMetadata);
            LOGGER.info("registering resources {}", entityType.getName());
        }
        processAssociations();
    }

    private void processAssociations() {
        for (JpaEntityMetadata entityMetadata : entityMap.values()) {
            JpaEntityProcessor.processAssociations(entityMetadata, typeMap);
        }
    }

    @Override
    public Collection<EntityMetadata> getAllEntityMetadata() {
        return entityMap.values().stream().map(em -> (EntityMetadata) em).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public boolean hasEntityMetadata(Class<?> entityType) {
        return typeMap.containsKey(entityType);
    }

    @Override
    public EntityMetadata findEntityMetadata(Class<?> entityType) throws NotFoundException {
        return typeMap.get(entityType);
    }

    @Override
    public boolean hasEntityMetadata(String entityName) {
        return entityMap.containsKey(entityName);
    }

    @Override
    public EntityMetadata findEntityMetadata(String entityName) throws NotFoundException {
        if (entityMap.containsKey(entityName)) {
            return entityMap.get(entityName);
        }
        throw new NotFoundException(entityName);
    }

    @Override
    public FieldMetadata findFieldMetadata(String entityName, String fieldName) throws NotFoundException {
        EntityMetadata entityMetadata = findEntityMetadata(entityName);
        return entityMetadata.field(entityName);
    }

    @Override
    public QueryProcessor getQueryProcess(EntityManager entityManager) {
        return new JpaQueryProcessorImpl(entityManager);
    }


}
