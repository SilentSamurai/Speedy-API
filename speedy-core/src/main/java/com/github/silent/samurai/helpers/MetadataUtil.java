package com.github.silent.samurai.helpers;

import com.github.silent.samurai.deserializer.JsonEntityDeserializer;
import com.github.silent.samurai.deserializer.JsonEntityKeyDeserializer;
import com.github.silent.samurai.deserializer.MapEntityDeserializer;
import com.github.silent.samurai.deserializer.MapEntityKeyDeserializer;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;

import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MetadataUtil {

    public static Object createEntityObjectFromMap(EntityMetadata entityMetadata, HashMap hashMap, EntityManager entityManager) throws Exception {
        MapEntityDeserializer deserializer = new MapEntityDeserializer(hashMap, entityMetadata, entityManager);
        return deserializer.deserialize();
    }

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

    public static Object createEntityKeyFromMap(Map<String, String> keywords, EntityMetadata entityMetadata) throws Exception {
        MapEntityKeyDeserializer deserializer = new MapEntityKeyDeserializer(keywords, entityMetadata);
        return deserializer.deserialize();
    }

    public static Object createEntityFromJSON(EntityMetadata entityMetadata, JsonObject asJsonObject, EntityManager entityManager) throws Exception {
        JsonEntityDeserializer deserializer = new JsonEntityDeserializer(asJsonObject, entityMetadata, entityManager);
        return deserializer.deserialize();
    }

    public static void updateEntityFromJson(EntityMetadata entityMetadata,
                                            EntityManager entityManager,
                                            JsonObject asJsonObject,
                                            Object entityInstance) throws Exception {
        JsonEntityDeserializer deserializer = new JsonEntityDeserializer(asJsonObject, entityMetadata, entityManager);
        deserializer.deserializeOn(entityInstance);
    }

    public static Object createEntityKeyFromJSON(EntityMetadata entityMetadata, JsonObject keyJson) throws Exception {
        JsonEntityKeyDeserializer deserializer = new JsonEntityKeyDeserializer(entityMetadata, keyJson);
        return deserializer.deserialize();
    }

    public String getEntityNameFromType(Class<?> entityType) {
        return entityType.getSimpleName();
    }

}
