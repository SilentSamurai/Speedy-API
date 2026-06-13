package com.github.silent.samurai.speedy.helpers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.conversion.registry.JsonRegistry;
import com.github.silent.samurai.speedy.conversion.walker.json.JsonToSpeedy;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.models.SpeedyNull;

import java.util.HashSet;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MetadataUtil {

    public static boolean isPrimaryKeyComplete(EntityMetadata entityMetadata, ObjectNode objectNode) {
        Set<String> validFieldnames = StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(objectNode.fieldNames(), Spliterator.ORDERED),
                        false)
                .filter(field -> !objectNode.get(field).isNull())
                .collect(Collectors.toSet());
        validFieldnames.retainAll(entityMetadata.getKeyFieldNames());
        return validFieldnames.size() == entityMetadata.getKeyFields().size();
    }

    public static boolean hasOnlyPrimaryKeyFields(EntityMetadata entityMetadata, Set<String> fields) {
        if (!fields.isEmpty() && entityMetadata.getKeyFieldNames().size() == fields.size()) {
            Set<String> fieldCopy = new HashSet<>(fields);
            fieldCopy.retainAll(entityMetadata.getKeyFieldNames());
            return fieldCopy.size() == entityMetadata.getKeyFieldNames().size();
        }
        return false;
    }

    /// Parses a JSON object node into a populated SpeedyEntity using
    /// the provided JSON registry for scalar value decoding.
    ///
    /// @param entityMetadata metadata of the target entity
    /// @param jsonObject     the JSON object containing field values
    /// @param jsonRegistry   registry used by {@link JsonToSpeedy} for decoding
    /// @return a populated SpeedyEntity
    /// @throws SpeedyHttpException if JSON parsing fails
    public static SpeedyEntity createEntityFromJSON(EntityMetadata entityMetadata, ObjectNode jsonObject, JsonRegistry jsonRegistry) throws SpeedyHttpException {
        return new JsonToSpeedy(jsonRegistry).fromEntityMetadata(entityMetadata, jsonObject);
    }

    /// Parses a JSON object node into a SpeedyEntityKey (PK only) using
    /// the provided JSON registry for scalar value decoding.
    ///
    /// @param entityMetadata metadata of the target entity
    /// @param keyJson        the JSON object containing key field values
    /// @param jsonRegistry   registry used by {@link JsonToSpeedy} for decoding
    /// @return a populated SpeedyEntityKey
    /// @throws SpeedyHttpException if JSON parsing fails
    public static SpeedyEntityKey createIdentifierFromJSON(EntityMetadata entityMetadata, ObjectNode keyJson, JsonRegistry jsonRegistry) throws SpeedyHttpException {
        return new JsonToSpeedy(jsonRegistry).fromPkJson(entityMetadata, keyJson);
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

//    public String getEntityNameFromType(Class<?> entityType) {
//        return entityType.getSimpleName();
//    }

}
