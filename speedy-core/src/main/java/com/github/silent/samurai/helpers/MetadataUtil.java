package com.github.silent.samurai.helpers;

import com.github.silent.samurai.deserializer.JsonEntityDeserializer;
import com.github.silent.samurai.deserializer.JsonIdentityDeserializer;
import com.github.silent.samurai.deserializer.ParserIdentityDeserializer;
import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.parser.SpeedyUriParser;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;

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

    public static Object createIdentifierFromParser(SpeedyUriParser parser) throws Exception {
        try {
            ParserIdentityDeserializer deserializer = new ParserIdentityDeserializer(parser);
            return deserializer.deserialize();
        } catch (Exception e) {
            throw new BadRequestException("failed to parse parameters");
        }
    }

//    public static Object createEntityFromMap(EntityMetadata entityMetadata, HashMap hashMap, EntityManager entityManager) throws Exception {
//        MapEntityDeserializer deserializer = new MapEntityDeserializer(hashMap, entityMetadata, entityManager);
//        return deserializer.deserialize();
//    }

    public static Object createEntityFromJSON(EntityMetadata entityMetadata, JsonObject asJsonObject, EntityManager entityManager) throws Exception {
        try {
            JsonEntityDeserializer deserializer = new JsonEntityDeserializer(asJsonObject, entityMetadata, entityManager);
            return deserializer.deserialize();
        } catch (Exception e) {
            throw new BadRequestException("failed to parse body");
        }
    }

    public static void updateEntityFromJSON(EntityMetadata entityMetadata,
                                            EntityManager entityManager,
                                            JsonObject asJsonObject,
                                            Object entityInstance) throws Exception {
        try {
            JsonEntityDeserializer deserializer = new JsonEntityDeserializer(asJsonObject, entityMetadata, entityManager);
            deserializer.deserializeOn(entityInstance);
        } catch (Exception e) {
            throw new BadRequestException("failed to parse body");
        }
    }

    public static Object createIdentifierFromJSON(EntityMetadata entityMetadata, JsonObject keyJson) throws Exception {
        try {
            JsonIdentityDeserializer deserializer = new JsonIdentityDeserializer(entityMetadata, keyJson);
            return deserializer.deserialize();
        } catch (Exception e) {
            throw new BadRequestException("failed to parse body");
        }
    }

    public String getEntityNameFromType(Class<?> entityType) {
        return entityType.getSimpleName();
    }

}
