package com.github.silent.samurai.serializers;

import com.github.silent.samurai.metamodel.JpaMetaModel;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

public class ApiAutomateJsonSerializer {

    public static int SINGLE_ENTITY = 0;
    public static int MULTIPLE_ENTITY = 1;

    private final JpaMetaModel jpaMetaModel;
    private int level = 0;

    public ApiAutomateJsonSerializer(JpaMetaModel jpaMetaModel) {
        this.jpaMetaModel = jpaMetaModel;
    }


    public JsonObject fromObject(Object entityObject, Class<?> clazz, int serializedType) throws InvocationTargetException, IllegalAccessException {
        JsonObject json = new JsonObject();
        JpaMetaModel.EntityMetadata entityMetadata = jpaMetaModel.getEntityMetadata(clazz.getSimpleName());
        Gson gson = new Gson();
        level++;


        for (JpaMetaModel.MemberMetadata memberMetadata : entityMetadata.membersMetadata) {
            Object value = memberMetadata.getFieldValue(entityObject);
            if (memberMetadata.jpaAttribute.isAssociation()) {
                if (serializedType == SINGLE_ENTITY && level < 2) {
                    if (memberMetadata.jpaAttribute.isCollection()) {
                        JsonArray jsonArray = formCollection((Collection<?>) value, serializedType);
                        json.add(memberMetadata.name, jsonArray);
                    } else {
                        JsonObject jsonObject = fromObject(value, value.getClass(), serializedType);
                        json.add(memberMetadata.name, jsonObject);
                    }
                }
            } else if (memberMetadata.jpaAttribute.isCollection()) {
                JsonArray jsonArray = formCollection((Collection<?>) value, serializedType);
                json.add(memberMetadata.name, jsonArray);
            } else {
                JsonElement jsonElement = gson.toJsonTree(value);
                json.add(memberMetadata.name, jsonElement);
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
