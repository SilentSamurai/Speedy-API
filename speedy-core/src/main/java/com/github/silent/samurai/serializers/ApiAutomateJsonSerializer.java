package com.github.silent.samurai.serializers;

import com.github.silent.samurai.metamodel.JpaMetaModel;
import com.github.silent.samurai.metamodel.MemberMetadata;
import com.github.silent.samurai.metamodel.ResourceMetadata;
import com.github.silent.samurai.utils.CommonUtil;
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
        ResourceMetadata entityMetadata = jpaMetaModel.getEntityMetadata(clazz.getSimpleName());
        Gson gson = CommonUtil.getGson();
        level++;


        for (MemberMetadata memberMetadata : entityMetadata.getMembersMetadata()) {
            Object value = memberMetadata.getFieldValue(entityObject);
            if (memberMetadata.getJpaAttribute().isAssociation()) {
                if (serializedType == SINGLE_ENTITY && level < 2) {
                    if (memberMetadata.getJpaAttribute().isCollection()) {
                        JsonArray jsonArray = formCollection((Collection<?>) value, serializedType);
                        json.add(memberMetadata.getName(), jsonArray);
                    } else {
                        JsonObject jsonObject = fromObject(value, value.getClass(), serializedType);
                        json.add(memberMetadata.getName(), jsonObject);
                    }
                }
            } else if (memberMetadata.getJpaAttribute().isCollection()) {
                JsonArray jsonArray = formCollection((Collection<?>) value, serializedType);
                json.add(memberMetadata.getName(), jsonArray);
            } else {
                JsonElement jsonElement = gson.toJsonTree(value);
                json.add(memberMetadata.getName(), jsonElement);
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
