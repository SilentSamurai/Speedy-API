package com.github.silent.samurai.speedy.serializers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;
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

            if (fieldMetadata.isAssociation()) {
                if (level < 1) {
                    if (fieldMetadata.isCollection()) {
                        if (serializedType == IResponseSerializer.SINGLE_ENTITY) {
                            Collection<SpeedyEntity> value = speedyEntity.getManyAssociatedValue(fieldMetadata);
                            if (value != null) {
                                ArrayNode childArray = formCollection(value, fieldMetadata.getAssociationMetadata(), serializedType, level + 1);
                                jsonObject.set(fieldMetadata.getClassFieldName(), childArray);
                            }
                        }
                    } else {
                        SpeedyEntity value = speedyEntity.getOneAssociatedValue(fieldMetadata);
                        if (value != null) {
                            ObjectNode childObject = fromSpeedyEntity(value, fieldMetadata.getAssociationMetadata(), serializedType, level + 1);
                            jsonObject.set(fieldMetadata.getClassFieldName(), childObject);
                        }
                    }
                }
            } else if (fieldMetadata.isCollection() && !fieldMetadata.isAssociation()) {
                SpeedyValue value = speedyEntity.getManyBasicValue(fieldMetadata);
                if (value != null) {
                    ArrayNode jsonArray = formCollectionOfBasics(value.getValues());
                    jsonObject.set(fieldMetadata.getClassFieldName(), jsonArray);
                }
            } else {
                SpeedyValue value = speedyEntity.getBasicValue(fieldMetadata);
                if (value != null) {
                    JsonNode jsonElement = json.valueToTree(value.getSingleValue());
                    jsonObject.set(fieldMetadata.getOutputPropertyName(), jsonElement);
                }
            }
        }
        return jsonObject;
    }

    public ArrayNode formCollectionOfBasics(Collection<?> collection) {
        ArrayNode jsonArray = json.createArrayNode();
        for (Object value : collection) {
            JsonNode jsonElement = json.valueToTree(value);
            jsonArray.add(jsonElement);
        }
        return jsonArray;
    }

    public ArrayNode formCollection(Collection<SpeedyEntity> collection, EntityMetadata entityMetadata, int serializedType, int level) throws InvocationTargetException, IllegalAccessException, NotFoundException {
        ArrayNode jsonArray = json.createArrayNode();
        for (SpeedyEntity object : collection) {
            JsonNode jsonObject = fromSpeedyEntity(object, entityMetadata, serializedType, level);
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }
}
