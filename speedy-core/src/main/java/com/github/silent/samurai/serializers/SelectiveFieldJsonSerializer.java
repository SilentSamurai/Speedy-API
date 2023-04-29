package com.github.silent.samurai.serializers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.exceptions.NotFoundException;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.FieldMetadata;
import com.github.silent.samurai.interfaces.IResponseSerializer;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.speedy.utils.CommonUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.function.Predicate;

public class SelectiveFieldJsonSerializer {

    private final MetaModelProcessor metaModelProcessor;
    private final Predicate<FieldMetadata> fieldPredicate;
    private int level = 0;
    private static final ObjectMapper json = CommonUtil.json();

    public SelectiveFieldJsonSerializer(MetaModelProcessor metaModelProcessor, Predicate<FieldMetadata> fieldPredicate) {
        this.metaModelProcessor = metaModelProcessor;
        this.fieldPredicate = fieldPredicate;
    }

    public ObjectNode fromObject(Object entityObject, Class<?> clazz, int serializedType) throws InvocationTargetException, IllegalAccessException, NotFoundException {
        ObjectNode jsonObject = json.createObjectNode();
        EntityMetadata entityMetadata = metaModelProcessor.findEntityMetadata(clazz.getSimpleName());
        level++;
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            if (!fieldMetadata.isSerializable() || !this.fieldPredicate.test(fieldMetadata)) continue;
            Object value = fieldMetadata.getEntityFieldValue(entityObject);
            if (fieldMetadata.isAssociation()) {
                if (serializedType == IResponseSerializer.SINGLE_ENTITY && level < 2) {
                    if (fieldMetadata.isCollection()) {
                        ArrayNode childArray = formCollection((Collection<?>) value, serializedType);
                        jsonObject.set(fieldMetadata.getClassFieldName(), childArray);
                    } else {
                        ObjectNode childObject = fromObject(value, value.getClass(), serializedType);
                        jsonObject.set(fieldMetadata.getClassFieldName(), childObject);
                    }
                }
            } else if (fieldMetadata.isCollection()) {
                ArrayNode jsonArray = formCollection((Collection<?>) value, serializedType);
                jsonObject.set(fieldMetadata.getClassFieldName(), jsonArray);
            } else {
                JsonNode jsonElement = json.valueToTree(value);
                jsonObject.set(fieldMetadata.getOutputPropertyName(), jsonElement);
            }
        }
        return jsonObject;
    }

    public ArrayNode formCollection(Collection<?> collection, int serializedType) throws InvocationTargetException, IllegalAccessException, NotFoundException {
        ArrayNode jsonArray = json.createArrayNode();
        for (Object object : collection) {
            JsonNode jsonObject = fromObject(object, object.getClass(), serializedType);
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }
}
