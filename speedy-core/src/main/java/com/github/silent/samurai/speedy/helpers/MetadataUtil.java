package com.github.silent.samurai.speedy.helpers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.models.SpeedyNull;
import com.github.silent.samurai.speedy.utils.SpeedyValueFactory;

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

    public static SpeedyEntity createEntityFromJSON(EntityMetadata entityMetadata, ObjectNode jsonObject) throws SpeedyHttpException {
        return SpeedyValueFactory.fromJsonObject(entityMetadata, jsonObject);
    }

    public static SpeedyEntityKey createIdentifierFromJSON(EntityMetadata entityMetadata, ObjectNode keyJson) throws SpeedyHttpException {
        return SpeedyValueFactory.fromPkJson(entityMetadata, keyJson);
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
