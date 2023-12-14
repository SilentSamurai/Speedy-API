package com.github.silent.samurai.speedy.serializers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.models.*;
import com.github.silent.samurai.speedy.utils.CommonUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.function.Predicate;

public class SelectiveFieldJsonSerializer {

    private final MetaModelProcessor metaModelProcessor;
    private final Predicate<FieldMetadata> fieldPredicate;
    private static final ObjectMapper json = CommonUtil.json();

    public SelectiveFieldJsonSerializer(MetaModelProcessor metaModelProcessor, Predicate<FieldMetadata> fieldPredicate) {
        this.metaModelProcessor = metaModelProcessor;
        this.fieldPredicate = fieldPredicate;
    }

    public ObjectNode fromSpeedyEntity(SpeedyEntity speedyEntity, EntityMetadata entityMetadata, int serializedType, int level) throws InvocationTargetException, IllegalAccessException, NotFoundException {
        ObjectNode jsonObject = json.createObjectNode();
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            if (!fieldMetadata.isSerializable() || !this.fieldPredicate.test(fieldMetadata)) continue;
            if (!speedyEntity.has(fieldMetadata)) {
                jsonObject.putNull(fieldMetadata.getOutputPropertyName());
                continue;
            }
            if (fieldMetadata.isAssociation()) {
                if (level < 1) {
                    if (fieldMetadata.isCollection()) {
                        if (serializedType == IResponseSerializer.SINGLE_ENTITY) {
                            SpeedyCollection speedyValue = (SpeedyCollection) speedyEntity.get(fieldMetadata);
                            Collection<SpeedyValue> value = speedyValue.getValue();
                            if (value != null) {
                                ArrayNode childArray = formCollection(value, fieldMetadata.getAssociationMetadata(), serializedType, level + 1);
                                jsonObject.set(fieldMetadata.getClassFieldName(), childArray);
                            }
                        }
                    } else {
                        SpeedyEntity value = (SpeedyEntity) speedyEntity.get(fieldMetadata);
                        if (value != null) {
                            ObjectNode childObject = fromSpeedyEntity(value, fieldMetadata.getAssociationMetadata(), serializedType, level + 1);
                            jsonObject.set(fieldMetadata.getClassFieldName(), childObject);
                        }
                    }
                }
            } else if (fieldMetadata.isCollection() && !fieldMetadata.isAssociation()) {
                SpeedyCollection speedyValue = (SpeedyCollection) speedyEntity.get(fieldMetadata);
                if (!speedyValue.isEmpty()) {
                    Collection<SpeedyValue> value = speedyValue.getValue();
                    ArrayNode jsonArray = formCollectionOfBasics(fieldMetadata, value);
                    jsonObject.set(fieldMetadata.getClassFieldName(), jsonArray);
                }
            } else {
                SpeedyValue value = speedyEntity.get(fieldMetadata);
                if (!value.isEmpty()) {
                    fromBasic(fieldMetadata, value, jsonObject);
                }
            }
        }
        return jsonObject;
    }

    public void fromBasic(FieldMetadata fieldMetadata, SpeedyValue speedyValue, ObjectNode jsonObject) {
        switch (fieldMetadata.getValueType()) {
            case TEXT:
                SpeedyText speedyText = (SpeedyText) speedyValue;
                jsonObject.put(fieldMetadata.getOutputPropertyName(), speedyText.getValue());
                break;
            case INT:
                SpeedyInt speedyInt = (SpeedyInt) speedyValue;
                jsonObject.put(fieldMetadata.getOutputPropertyName(), speedyInt.getValue());
                break;
            case FLOAT:
                SpeedyDouble speedyDouble = (SpeedyDouble) speedyValue;
                jsonObject.put(fieldMetadata.getOutputPropertyName(), speedyDouble.getValue());
                break;
            case DATE:
                SpeedyDate speedyDate = (SpeedyDate) speedyValue;
                jsonObject.put(fieldMetadata.getOutputPropertyName(), speedyDate.getValue().toString());
                break;
            case TIME:
                SpeedyTime speedyTime = (SpeedyTime) speedyValue;
                jsonObject.put(fieldMetadata.getOutputPropertyName(), speedyTime.getValue().toString());
                break;
            case DATE_TIME:
                SpeedyDateTime speedyDateTime = (SpeedyDateTime) speedyValue;
                jsonObject.put(fieldMetadata.getOutputPropertyName(), speedyDateTime.getValue().toString());
                break;
            case NULL:
                jsonObject.putNull(fieldMetadata.getOutputPropertyName());
                break;
            case OBJECT:
            case COLLECTION:
            default:
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

    public ArrayNode formCollection(Collection<SpeedyValue> collection, EntityMetadata entityMetadata, int serializedType, int level) throws InvocationTargetException, IllegalAccessException, NotFoundException {
        ArrayNode jsonArray = json.createArrayNode();
        for (SpeedyValue object : collection) {
            SpeedyEntity speedyEntity = (SpeedyEntity) object;
            JsonNode jsonObject = fromSpeedyEntity(speedyEntity, entityMetadata, serializedType, level);
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }
}
