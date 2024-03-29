package com.github.silent.samurai.speedy.helpers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.deserializer.JsonEntityDeserializer;
import com.github.silent.samurai.speedy.deserializer.JsonIdentityDeserializer;
import com.github.silent.samurai.speedy.deserializer.ParserIdentityDeserializer;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.google.common.collect.Sets;

import javax.persistence.EntityManager;
import java.util.Set;

public class MetadataUtil {

    public static boolean isPrimaryKeyComplete(EntityMetadata entityMetadata, Set<String> fields) {
        Sets.SetView<String> difference = Sets.intersection(entityMetadata.getKeyFieldNames(), fields);
        return difference.size() == entityMetadata.getKeyFields().size();
    }

    public static boolean hasOnlyPrimaryKeyFields(EntityMetadata entityMetadata, Set<String> fields) {
        if (!fields.isEmpty() && entityMetadata.getKeyFieldNames().size() == fields.size()) {
            Sets.SetView<String> difference = Sets.intersection(entityMetadata.getKeyFieldNames(), fields);
            return difference.size() == entityMetadata.getKeyFieldNames().size();
        }
        return false;
    }

    public static Object createIdentifierFromParser(SpeedyUriContext parser) throws Exception {
        try {
            ParserIdentityDeserializer deserializer = new ParserIdentityDeserializer(parser.getPrimaryResource());
            return deserializer.deserialize();
        } catch (Exception e) {
            throw new BadRequestException("failed to parse parameters", e);
        }
    }

//    public static Object createEntityFromMap(EntityMetadata entityMetadata, HashMap hashMap, EntityManager entityManager) throws Exception {
//        MapEntityDeserializer deserializer = new MapEntityDeserializer(hashMap, entityMetadata, entityManager);
//        return deserializer.deserialize();
//    }

    public static Object createEntityFromJSON(EntityMetadata entityMetadata, ObjectNode asJsonObject, EntityManager entityManager) throws Exception {
        try {
            JsonEntityDeserializer deserializer = new JsonEntityDeserializer(asJsonObject, entityMetadata, entityManager);
            return deserializer.deserialize();
        } catch (Exception e) {
            throw new BadRequestException("failed to parse body", e);
        }
    }

    public static void updateEntityFromJSON(EntityMetadata entityMetadata,
                                            EntityManager entityManager,
                                            ObjectNode asJsonObject,
                                            Object entityInstance) throws Exception {
        try {
            JsonEntityDeserializer deserializer = new JsonEntityDeserializer(asJsonObject, entityMetadata, entityManager);
            deserializer.deserializeOn(entityInstance);
        } catch (Exception e) {
            throw new BadRequestException("failed to parse body");
        }
    }

    public static Object createIdentifierFromJSON(EntityMetadata entityMetadata, ObjectNode keyJson) throws Exception {
        try {
            JsonIdentityDeserializer deserializer = new JsonIdentityDeserializer(entityMetadata, keyJson);
            return deserializer.deserialize();
        } catch (Exception e) {
            throw new BadRequestException("failed to parse body", e);
        }
    }

    public static boolean isKeyCompleteInEntity(EntityMetadata entityMetadata, Object entityInstance) {
        for (KeyFieldMetadata keyField : entityMetadata.getKeyFields()) {
            Object keyFieldValue = keyField.getIdFieldValue(entityInstance);
            if (keyFieldValue == null) {
                return false;
            }
        }
        return true;
    }

    public String getEntityNameFromType(Class<?> entityType) {
        return entityType.getSimpleName();
    }

}
