package com.github.silent.samurai.speedy.helpers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.deserializer.QueryKeyDeserializer;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.models.SpeedyNull;
import com.github.silent.samurai.speedy.utils.SpeedyValueFactory;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;

import java.util.Set;
import java.util.stream.Collectors;

public class MetadataUtil {

    public static boolean isPrimaryKeyComplete(EntityMetadata entityMetadata, ObjectNode objectNode) {
        Set<String> validFieldnames = Streams.stream(objectNode.fieldNames())
                .filter(field -> !objectNode.get(field).isNull())
                .collect(Collectors.toSet());
        Sets.SetView<String> difference = Sets.intersection(entityMetadata.getKeyFieldNames(), validFieldnames);
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
            throw new BadRequestException("failed to parse parameters : " + e.getMessage(), e);
        }
    }

    public static SpeedyEntity createEntityFromJSON(EntityMetadata entityMetadata, ObjectNode jsonObject) throws Exception {
        try {
            return SpeedyValueFactory.fromJsonObject(entityMetadata, jsonObject);
        } catch (Exception e) {
            throw new BadRequestException("failed to parse body : " + e.getMessage(), e);
        }
    }

    public static SpeedyEntityKey createIdentifierFromJSON(EntityMetadata entityMetadata, ObjectNode keyJson) throws Exception {
        try {
            return SpeedyValueFactory.fromPkJson(entityMetadata, keyJson);
        } catch (Exception e) {
            throw new BadRequestException("failed to parse body : " + e.getMessage(), e);
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
