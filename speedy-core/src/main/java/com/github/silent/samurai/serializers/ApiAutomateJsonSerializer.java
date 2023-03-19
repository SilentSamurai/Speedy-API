package com.github.silent.samurai.serializers;

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

public class ApiAutomateJsonSerializer {

    private final MetaModelProcessor metaModelProcessor;
    private int level = 0;

    public ApiAutomateJsonSerializer(MetaModelProcessor metaModelProcessor) {
        this.metaModelProcessor = metaModelProcessor;
    }


    public JsonObject fromObject(Object entityObject, Class<?> clazz, int serializedType) throws InvocationTargetException, IllegalAccessException {
        JsonObject json = new JsonObject();
        EntityMetadata entityMetadata = metaModelProcessor.findEntityMetadata(clazz.getSimpleName());
        Gson gson = CommonUtil.getGson();
        level++;


        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            Object value = fieldMetadata.getClassFieldValue(entityObject);
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

    public JsonArray formCollection(Collection<?> collection, int serializedType) throws InvocationTargetException, IllegalAccessException {
        JsonArray jsonArray = new JsonArray();
        for (Object object : collection) {
            JsonObject jsonObject = fromObject(object, object.getClass(), serializedType);
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }
}
