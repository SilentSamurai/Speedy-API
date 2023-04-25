package com.github.silent.samurai.serializers;

import com.github.silent.samurai.exceptions.NotFoundException;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.FieldMetadata;
import com.github.silent.samurai.interfaces.IResponseSerializer;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.utils.CommonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.function.Predicate;

public class SelectiveFieldJsonSerializer {

    private final MetaModelProcessor metaModelProcessor;
    private final Predicate<FieldMetadata> fieldPredicate;
    private int level = 0;

    public SelectiveFieldJsonSerializer(MetaModelProcessor metaModelProcessor, Predicate<FieldMetadata> fieldPredicate) {
        this.metaModelProcessor = metaModelProcessor;
        this.fieldPredicate = fieldPredicate;
    }

    public JsonObject fromObject(Object entityObject, Class<?> clazz, int serializedType) throws InvocationTargetException, IllegalAccessException, NotFoundException {
        JsonObject json = new JsonObject();
        EntityMetadata entityMetadata = metaModelProcessor.findEntityMetadata(clazz.getSimpleName());
        Gson gson = CommonUtil.getGson();
        level++;

        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            if (!fieldMetadata.isSerializable() || !this.fieldPredicate.test(fieldMetadata)) continue;
            Object value = fieldMetadata.getEntityFieldValue(entityObject);
            if (fieldMetadata.isAssociation()) {
                if (serializedType == IResponseSerializer.SINGLE_ENTITY && level < 2) {
                    if (fieldMetadata.isCollection()) {
                        JsonArray jsonArray = formCollection((Collection<?>) value, serializedType);
                        json.add(fieldMetadata.getClassFieldName(), jsonArray);
                    } else {
                        JsonObject jsonObject = fromObject(value, value.getClass(), serializedType);
                        json.add(fieldMetadata.getClassFieldName(), jsonObject);
                    }
                }
            } else if (fieldMetadata.isCollection()) {
                JsonArray jsonArray = formCollection((Collection<?>) value, serializedType);
                json.add(fieldMetadata.getClassFieldName(), jsonArray);
            } else {
                JsonElement jsonElement = gson.toJsonTree(value);
                json.add(fieldMetadata.getClassFieldName(), jsonElement);
            }
        }

        return json;
    }

    public JsonArray formCollection(Collection<?> collection, int serializedType) throws InvocationTargetException, IllegalAccessException, NotFoundException {
        JsonArray jsonArray = new JsonArray();
        for (Object object : collection) {
            JsonObject jsonObject = fromObject(object, object.getClass(), serializedType);
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }
}
