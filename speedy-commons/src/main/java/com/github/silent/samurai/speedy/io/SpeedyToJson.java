package com.github.silent.samurai.speedy.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.mappings.JsonRegistry;
import com.github.silent.samurai.speedy.models.ExpansionPathTracker;
import com.github.silent.samurai.speedy.models.SpeedyCollection;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.utils.CommonUtil;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

public class SpeedyToJson {

    private static final ObjectMapper json = CommonUtil.json();
    private final Predicate<FieldMetadata> fieldPredicate;
    /// The JSON registry used for encoding SpeedyValue instances into JSON-compatible objects.
    /// Consulted by {@link #fromBasic} to find the appropriate codec per value type.
    ///
    /// @see JsonRegistry
    private final JsonRegistry jsonRegistry;

    /// Creates a serializer with the given field-level predicate and JSON registry.
    ///
    /// @param metaModel      the metamodel (unused directly but retained for compatibility)
    /// @param fieldPredicate predicate that determines which fields to include in output
    /// @param jsonRegistry   the registry used for JSON encoding
    public SpeedyToJson(MetaModel metaModel, Predicate<FieldMetadata> fieldPredicate, JsonRegistry jsonRegistry) {
        this.fieldPredicate = fieldPredicate;
        this.jsonRegistry = jsonRegistry;
    }

    public ObjectNode fromSpeedyEntity(SpeedyEntity speedyEntity,
                                       EntityMetadata entityMetadata,
                                       Set<String> expand) throws SpeedyHttpException {
        return fromSpeedyEntity(speedyEntity, entityMetadata, new ExpansionPathTracker(expand));
    }

    private ObjectNode fromSpeedyEntity(SpeedyEntity speedyEntity,
                                        EntityMetadata entityMetadata,
                                        ExpansionPathTracker pathTracker) throws SpeedyHttpException {
        ObjectNode jsonObject = json.createObjectNode();

        // Push the current entity onto the path tracker
        pathTracker.pushEntity(entityMetadata);

        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            if (!fieldMetadata.isSerializable() || !this.fieldPredicate.test(fieldMetadata)) continue;
            if (!speedyEntity.has(fieldMetadata)) {
                jsonObject.putNull(fieldMetadata.getOutputPropertyName());
                continue;
            }
            if (fieldMetadata.isAssociation()) {
                String associationName = fieldMetadata.getAssociationMetadata().getName();

                // Check if this specific path should be expanded using dot notation
                if (pathTracker.shouldExpand(fieldMetadata.getAssociationMetadata())) {
                    if (fieldMetadata.isCollection()) {
                        Collection<SpeedyValue> value = speedyEntity.get(fieldMetadata).asCollection();
                        if (value != null) {
                            ArrayNode childArray = formCollection(value,
                                    fieldMetadata.getAssociationMetadata(),
                                    pathTracker);
                            jsonObject.set(fieldMetadata.getOutputPropertyName(), childArray);
                        }
                    } else {
                        if (speedyEntity.has(fieldMetadata) && speedyEntity.get(fieldMetadata) != null) {
                            // if association is not present
                            if (speedyEntity.get(fieldMetadata).isObject()) {
                                SpeedyEntity speedyObject = speedyEntity.get(fieldMetadata).asObject();
                                ObjectNode childObject = fromSpeedyEntity(speedyObject,
                                        fieldMetadata.getAssociationMetadata(),
                                        pathTracker);
                                jsonObject.set(fieldMetadata.getOutputPropertyName(), childObject);
                            } else {
                                jsonObject.putNull(fieldMetadata.getOutputPropertyName());
                            }
                        }
                    }
                } else {
                    if (fieldMetadata.isCollection()) {
                        Collection<SpeedyValue> value = speedyEntity.get(fieldMetadata).asCollection();
                        if (!value.isEmpty()) {
                            ArrayNode childArray = onlyKeyCollection(value, fieldMetadata.getAssociationMetadata());
                            jsonObject.set(fieldMetadata.getOutputPropertyName(), childArray);
                        }
                    } else {
                        SpeedyValue fieldValue = speedyEntity.get(fieldMetadata);
                        if (fieldValue.isObject()) {
                            SpeedyEntity value = speedyEntity.get(fieldMetadata).asObject();
                            ObjectNode childObject = onlyKeys(value, fieldMetadata.getAssociationMetadata());
                            jsonObject.set(fieldMetadata.getOutputPropertyName(), childObject);
                        } else {
                            jsonObject.putNull(fieldMetadata.getOutputPropertyName());
                        }
                    }
                }
            } else if (fieldMetadata.isCollection() && !fieldMetadata.isAssociation()) {
                SpeedyCollection speedyValue = (SpeedyCollection) speedyEntity.get(fieldMetadata);
                if (!speedyValue.isEmpty()) {
                    Collection<SpeedyValue> value = speedyValue.asCollection();
                    ArrayNode jsonArray = formCollectionOfBasics(fieldMetadata, value);
                    jsonObject.set(fieldMetadata.getOutputPropertyName(), jsonArray);
                }
            } else {
                SpeedyValue value = speedyEntity.get(fieldMetadata);
                if (!value.isEmpty()) {
                    fromBasic(fieldMetadata, value, jsonObject);
                } else {
                    jsonObject.putNull(fieldMetadata.getOutputPropertyName());
                }
            }
        }

        // Pop current entity from the path tracker when done processing
        pathTracker.popEntity();
        return jsonObject;
    }

    public ArrayNode onlyKeyCollection(Collection<SpeedyValue> collection, EntityMetadata entityMetadata)
            throws SpeedyHttpException {
        ArrayNode jsonArray = json.createArrayNode();
        for (SpeedyValue speedyValue : collection) {
            if (speedyValue.isObject()) {
                SpeedyEntity speedyEntity = speedyValue.asObject();
                JsonNode jsonObject = onlyKeys(speedyEntity, entityMetadata);
                jsonArray.add(jsonObject);
            } else {
                jsonArray.addNull();
            }
        }
        return jsonArray;
    }

    public ObjectNode onlyKeys(SpeedyEntity speedyEntity, EntityMetadata entityMetadata) {
        ObjectNode jsonObject = json.createObjectNode();
        for (KeyFieldMetadata fieldMetadata : entityMetadata.getKeyFields()) {
            if (!fieldMetadata.isSerializable()) continue;
            if (!speedyEntity.has(fieldMetadata) || speedyEntity.get(fieldMetadata).isEmpty()) {
                jsonObject.putNull(fieldMetadata.getOutputPropertyName());
                continue;
            }
            SpeedyValue value = speedyEntity.get(fieldMetadata);
            fromBasic(fieldMetadata, value, jsonObject);
        }
        return jsonObject;
    }

    public void fromBasic(FieldMetadata fieldMetadata, SpeedyValue speedyValue, ObjectNode jsonObject) {
        Object encoded = jsonRegistry.encode(fieldMetadata.getValueType(), speedyValue);
        String name = fieldMetadata.getOutputPropertyName();
        if (encoded == null) {
            jsonObject.putNull(name);
        } else if (encoded instanceof Boolean b) {
            jsonObject.put(name, b);
        } else if (encoded instanceof Long l) {
            jsonObject.put(name, l);
        } else if (encoded instanceof Double d) {
            jsonObject.put(name, d);
        } else {
            jsonObject.put(name, String.valueOf(encoded));
        }
    }

    public ArrayNode formCollectionOfBasics(FieldMetadata fieldMetadata, Collection<SpeedyValue> collection) {
        ArrayNode jsonArray = json.createArrayNode();
        for (SpeedyValue value : collection) {
            ObjectNode jsonObject = json.createObjectNode();
            fromBasic(fieldMetadata, value, jsonObject);
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }

    public ArrayNode formCollection(Collection<? extends SpeedyValue> collection,
                                    EntityMetadata entityMetadata,
                                    Set<String> expands) throws SpeedyHttpException {
        return formCollection(collection, entityMetadata, new ExpansionPathTracker(expands));
    }

    public ArrayNode formCollection(Collection<? extends SpeedyValue> collection,
                                    EntityMetadata entityMetadata,
                                    ExpansionPathTracker pathTracker) throws SpeedyHttpException {
        ArrayNode jsonArray = json.createArrayNode();
        for (SpeedyValue object : collection) {
            if (object.isObject()) {
                SpeedyEntity speedyEntity = object.asObject();
                JsonNode jsonObject = fromSpeedyEntity(speedyEntity, entityMetadata, pathTracker);
                jsonArray.add(jsonObject);
            } else {
                jsonArray.addNull();
            }
        }
        return jsonArray;
    }
}
