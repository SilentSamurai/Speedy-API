package com.github.silent.samurai.speedy.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.models.*;
import com.github.silent.samurai.speedy.utils.CommonUtil;

import java.lang.reflect.InvocationTargetException;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class SelectiveSpeedy2Json {

    private final MetaModelProcessor metaModelProcessor;
    private final Predicate<FieldMetadata> fieldPredicate;
    private static final ObjectMapper json = CommonUtil.json();

    private final Set<String> expand = new HashSet<>();

    public SelectiveSpeedy2Json(MetaModelProcessor metaModelProcessor, Predicate<FieldMetadata> fieldPredicate) {
        this.metaModelProcessor = metaModelProcessor;
        this.fieldPredicate = fieldPredicate;
    }

    public ObjectNode fromSpeedyEntity(SpeedyEntity speedyEntity, EntityMetadata entityMetadata) throws InvocationTargetException, IllegalAccessException, NotFoundException {
        ObjectNode jsonObject = json.createObjectNode();
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            if (!fieldMetadata.isSerializable() || !this.fieldPredicate.test(fieldMetadata)) continue;
            if (!speedyEntity.has(fieldMetadata)) {
                jsonObject.putNull(fieldMetadata.getOutputPropertyName());
                continue;
            }
            if (fieldMetadata.isAssociation()) {
                if (expand.contains(fieldMetadata.getAssociationMetadata().getName())) {
                    if (fieldMetadata.isCollection()) {
                        Collection<SpeedyValue> value = speedyEntity.get(fieldMetadata).asCollection();
                        if (value != null) {
                            ArrayNode childArray = formCollection(value, fieldMetadata.getAssociationMetadata());
                            jsonObject.set(fieldMetadata.getOutputPropertyName(), childArray);
                        }
                    } else {
                        SpeedyEntity value = (SpeedyEntity) speedyEntity.get(fieldMetadata);
                        if (value != null) {
                            ObjectNode childObject = fromSpeedyEntity(value, fieldMetadata.getAssociationMetadata());
                            jsonObject.set(fieldMetadata.getOutputPropertyName(), childObject);
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
                        SpeedyEntity value = (SpeedyEntity) speedyEntity.get(fieldMetadata);
                        if (!value.isEmpty()) {
                            ObjectNode childObject = onlyKeys(value, fieldMetadata.getAssociationMetadata());
                            jsonObject.set(fieldMetadata.getOutputPropertyName(), childObject);
                        }
                    }
                }
            } else if (fieldMetadata.isCollection() && !fieldMetadata.isAssociation()) {
                SpeedyCollection speedyValue = (SpeedyCollection) speedyEntity.get(fieldMetadata);
                if (!speedyValue.isEmpty()) {
                    Collection<SpeedyValue> value = speedyValue.getValue();
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
        return jsonObject;
    }

    public ArrayNode onlyKeyCollection(Collection<SpeedyValue> collection, EntityMetadata entityMetadata) throws InvocationTargetException, IllegalAccessException, NotFoundException {
        ArrayNode jsonArray = json.createArrayNode();
        for (SpeedyValue object : collection) {
            SpeedyEntity speedyEntity = (SpeedyEntity) object;
            JsonNode jsonObject = onlyKeys(speedyEntity, entityMetadata);
            jsonArray.add(jsonObject);
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
        switch (fieldMetadata.getValueType()) {
            case BOOL:
                jsonObject.put(fieldMetadata.getOutputPropertyName(), speedyValue.asBoolean());
                break;
            case TEXT:
                jsonObject.put(fieldMetadata.getOutputPropertyName(), speedyValue.asText());
                break;
            case INT:
                jsonObject.put(fieldMetadata.getOutputPropertyName(), speedyValue.asLong());
                break;
            case FLOAT:
                jsonObject.put(fieldMetadata.getOutputPropertyName(), speedyValue.asDouble());
                break;
            case DATE: {
                String stringValue = speedyValue.asDate().format(DateTimeFormatter.ISO_DATE);
                jsonObject.put(fieldMetadata.getOutputPropertyName(), stringValue);
                break;
            }
            case TIME: {
                String stringValue = speedyValue.asTime().format(DateTimeFormatter.ISO_TIME);
                jsonObject.put(fieldMetadata.getOutputPropertyName(), stringValue);
                break;
            }
            case DATE_TIME: {
                String stringValue = speedyValue.asDateTime().format(DateTimeFormatter.ISO_DATE_TIME);
                jsonObject.put(fieldMetadata.getOutputPropertyName(), stringValue);
                break;
            }
            case ZONED_DATE_TIME: {
                String stringValue = speedyValue.asZonedDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                jsonObject.put(fieldMetadata.getOutputPropertyName(), stringValue);
                break;
            }
            case NULL:
                jsonObject.putNull(fieldMetadata.getOutputPropertyName());
                break;
            case OBJECT:
            case COLLECTION:
                break;
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

    public ArrayNode formCollection(Collection<? extends SpeedyValue> collection, EntityMetadata entityMetadata) throws InvocationTargetException, IllegalAccessException, NotFoundException {
        ArrayNode jsonArray = json.createArrayNode();
        for (SpeedyValue object : collection) {
            SpeedyEntity speedyEntity = (SpeedyEntity) object;
            JsonNode jsonObject = fromSpeedyEntity(speedyEntity, entityMetadata);
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }

    public void addExpand(Set<String> associationName) {
        this.expand.addAll(associationName);
    }
}
