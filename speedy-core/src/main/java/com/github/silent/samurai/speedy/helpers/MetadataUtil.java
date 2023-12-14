package com.github.silent.samurai.speedy.helpers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.deserializer.JsonEntityDeserializer;
import com.github.silent.samurai.speedy.deserializer.JsonIdentityDeserializer;
import com.github.silent.samurai.speedy.deserializer.QueryKeyDeserializer;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.models.SpeedyNull;
import com.google.common.collect.Sets;

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

    public static SpeedyEntityKey createIdentifierFromQuery(SpeedyQuery speedyQuery) throws Exception {
        try {
            QueryKeyDeserializer deserializer = new QueryKeyDeserializer(speedyQuery);
            return deserializer.deserialize();
        } catch (Exception e) {
            throw new BadRequestException("failed to parse parameters", e);
        }
    }

    public static SpeedyEntity createEntityFromJSON(EntityMetadata entityMetadata, ObjectNode asJsonObject) throws Exception {
        try {
            JsonEntityDeserializer deserializer = new JsonEntityDeserializer(asJsonObject, entityMetadata);
            return deserializer.deserialize();
        } catch (Exception e) {
            throw new BadRequestException("failed to parse body", e);
        }
    }

    public static SpeedyEntityKey createIdentifierFromJSON(EntityMetadata entityMetadata, ObjectNode keyJson) throws Exception {
        try {
            JsonIdentityDeserializer deserializer = new JsonIdentityDeserializer(entityMetadata, keyJson);
            return deserializer.deserialize();
        } catch (Exception e) {
            throw new BadRequestException("failed to parse body", e);
        }
    }

    public static boolean isKeyCompleteInEntity(EntityMetadata entityMetadata, SpeedyEntity entity) {
        for (KeyFieldMetadata keyField : entityMetadata.getKeyFields()) {
            SpeedyValue keyFieldValue = entity.get(keyField);
            if (keyFieldValue == SpeedyNull.SPEEDY_NULL) {
                return false;
            }
        }
        return true;
    }

    public String getEntityNameFromType(Class<?> entityType) {
        return entityType.getSimpleName();
    }

}
