package com.github.silent.samurai.serializers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializer;
import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
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

    public ObjectNode fromObject(Object entityObject, EntityMetadata entityMetadata, int serializedType, int level) throws InvocationTargetException, IllegalAccessException, NotFoundException {
        ObjectNode jsonObject = json.createObjectNode();

        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            if (!fieldMetadata.isSerializable() || !this.fieldPredicate.test(fieldMetadata)) continue;

            if (fieldMetadata.isAssociation()) {
                if (level < 1) {
                    if (fieldMetadata.isCollection()) {
                        if (serializedType == IResponseSerializer.SINGLE_ENTITY) {
                            // performance optimization as retrieving value can execute sql
                            Object value = fieldMetadata.getEntityFieldValue(entityObject);
                            if (value != null) {
                                ArrayNode childArray = formCollection((Collection<?>) value, fieldMetadata.getAssociationMetadata(), serializedType, level + 1);
                                jsonObject.set(fieldMetadata.getClassFieldName(), childArray);
                            }
                        }
                    } else {
                        // performance optimization as retrieving value can execute sql
                        Object value = fieldMetadata.getEntityFieldValue(entityObject);
                        if (value != null) {
                            ObjectNode childObject = fromObject(value, fieldMetadata.getAssociationMetadata(), serializedType, level + 1);
                            jsonObject.set(fieldMetadata.getClassFieldName(), childObject);
                        }
                    }
                }
            } else if (fieldMetadata.isCollection() && !fieldMetadata.isAssociation()) {
                // performance optimization as retrieving value can execute sql
                Object value = fieldMetadata.getEntityFieldValue(entityObject);
                if (value != null) {
                    ArrayNode jsonArray = formCollectionOfBasics((Collection<?>) value);
                    jsonObject.set(fieldMetadata.getClassFieldName(), jsonArray);
                }
            } else {
                // performance optimization as retrieving value can execute sql
                Object value = fieldMetadata.getEntityFieldValue(entityObject);
                if (value != null) {
                    JsonNode jsonElement = json.valueToTree(value);
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

    public ArrayNode formCollection(Collection<?> collection, EntityMetadata entityMetadata, int serializedType, int level) throws InvocationTargetException, IllegalAccessException, NotFoundException {
        ArrayNode jsonArray = json.createArrayNode();
        for (Object object : collection) {
            JsonNode jsonObject = fromObject(object, entityMetadata, serializedType, level);
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }
}
