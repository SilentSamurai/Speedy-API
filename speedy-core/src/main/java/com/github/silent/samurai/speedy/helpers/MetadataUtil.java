package com.github.silent.samurai.speedy.helpers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyNull;

import java.util.HashSet;
import java.util.Optional;
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

    public static boolean isKeyCompleteInEntity(EntityMetadata entityMetadata, SpeedyEntity entity) {
        for (KeyFieldMetadata keyField : entityMetadata.getKeyFields()) {
            SpeedyValue keyFieldValue = entity.get(keyField);
            if (keyFieldValue == SpeedyNull.SPEEDY_NULL) {
                return false;
            }
        }
        return true;
    }

    /// The first database-generated key ({@link KeyFieldMetadata#isDatabaseGenerated()}) that the
    /// backend did not populate after insert, if any. The persistence backend owns the read-back
    /// *mechanism* (RETURNING / LAST_INSERT_ID), but which keys the database assigns is backend-neutral
    /// metadata; core uses this to fail loudly and precisely when a backend skips the read-back, rather
    /// than surfacing a confusing downstream "row not found".
    public static Optional<KeyFieldMetadata> findUnpopulatedDatabaseGeneratedKey(EntityMetadata entityMetadata,
                                                                                 SpeedyEntity entity) {
        for (KeyFieldMetadata keyField : entityMetadata.getKeyFields()) {
            // A backend that read the key back will have put() it; an absent key is the failure we
            // want to catch (SpeedyEntity.get throws on an absent field, so check has() first).
            if (keyField.isDatabaseGenerated()
                    && (!entity.has(keyField) || entity.get(keyField) == SpeedyNull.SPEEDY_NULL)) {
                return Optional.of(keyField);
            }
        }
        return Optional.empty();
    }

//    public String getEntityNameFromType(Class<?> entityType) {
//        return entityType.getSimpleName();
//    }

}
