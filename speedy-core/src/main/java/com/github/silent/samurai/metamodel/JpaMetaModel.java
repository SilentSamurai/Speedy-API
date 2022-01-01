package com.github.silent.samurai.metamodel;

import com.github.silent.samurai.annotations.SpeedyCustomValidation;
import com.github.silent.samurai.interfaces.ISpeedyCustomValidation;
import com.github.silent.samurai.utils.CommonUtil;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.util.FieldUtils;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.SingularAttribute;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.*;


public class JpaMetaModel {

    public static Logger logger = LogManager.getLogger(JpaMetaModel.class);
    Map<String, EntityMetadata> entityMap = new HashMap<>();

    public static MemberMetadata findMetadata(Attribute<?, ?> attribute, Class<?> entityClass) {
        Member member = attribute.getJavaMember();
        MemberMetadata memberMetadata = new MemberMetadata();
        memberMetadata.jpaAttribute = attribute;
        memberMetadata.name = member.getName();
        if (attribute instanceof SingularAttribute) {
            memberMetadata.isId = ((SingularAttribute<?, ?>) attribute).isId();
        } else {
            memberMetadata.isId = false;
        }

        // MZ: Find the correct method
        for (Method method : entityClass.getMethods()) {
            if (method.getName().toLowerCase().endsWith(member.getName().toLowerCase())) {
                try {
                    if ((method.getName().startsWith("get")) && (method.getName().length() == (member.getName().length() + 3))) {
                        memberMetadata.getter = method;
                    }
                    if ((method.getName().startsWith("set")) && (method.getName().length() == (member.getName().length() + 3))) {
                        memberMetadata.setter = method;
                    }
                    memberMetadata.field = FieldUtils.getField(member.getDeclaringClass(), member.getName());
                    SpeedyCustomValidation annotation = AnnotationUtils.getAnnotation(memberMetadata.field, SpeedyCustomValidation.class);
                    if (annotation != null) {
                        memberMetadata.customValidation = annotation.value();
                    }

                    break;
                } catch (IllegalStateException e) {
                    logger.fatal("Could not determine method: {} ", member, e);
                }
            }
        }
        return memberMetadata;
    }

    public void addEntity(EntityType<?> entityType) {
        EntityMetadata metadata = new EntityMetadata();
        metadata.name = entityType.getName();
        metadata.jpaEntityType = entityType;
        metadata.membersMetadata = new HashSet<>();
        metadata.memberMap = new HashMap<>();
        metadata.idFields = new HashSet<>();
        for (Attribute<?, ?> attribute : entityType.getAttributes()) {
            MemberMetadata memberMetadata = findMetadata(attribute, entityType.getJavaType());
            metadata.membersMetadata.add(memberMetadata);
            metadata.memberMap.put(attribute.getName(), memberMetadata);
            if (memberMetadata.isId) {
                metadata.idFields.add(attribute.getName());
            }
        }
        entityMap.put(entityType.getName(), metadata);
    }

    public EntityMetadata getEntityMetadata(String entity) {
        return entityMap.get(entity);
    }

    public static class MemberMetadata {
        public Attribute<?, ?> jpaAttribute;
        public String name;
        public Method getter;
        public Method setter;
        public Field field;
        public boolean isId;
        public Class<ISpeedyCustomValidation> customValidation;

        public boolean isCustomValidationRequired() {
            return customValidation != null;
        }

        public Object getFieldValue(Object entityObject) throws IllegalAccessException, InvocationTargetException {
            return this.getter.invoke(entityObject);
        }
    }

    public static class EntityMetadata {
        public String name;
        public EntityType<?> jpaEntityType;
        public Set<MemberMetadata> membersMetadata;
        public Map<String, MemberMetadata> memberMap;
        public Set<String> idFields;

        public boolean isKeyField(String field) {
            return memberMap.get(field).isId;
        }

        public boolean isOnlyPrimaryKeyFields(Set<String> fields) {
            Sets.SetView<String> difference = Sets.difference(fields, idFields);
            return difference.isEmpty();
        }

        public boolean isPrimaryKeyComplete(Set<String> fields) {
            // returns fields present in idFields and not in fields
            Sets.SetView<String> difference = Sets.difference(idFields, fields);
            return difference.isEmpty();
        }

        public Object getPrimaryKeyObject(Map<String, ?> fieldsMap) {
            if (Objects.equals(jpaEntityType.getIdType().getJavaType(), String.class)) {
                return fieldsMap.get("id");
            }
            return CommonUtil.mapModel(fieldsMap, jpaEntityType.getIdType().getJavaType());
        }

        public Object getPrimaryKeyObject(JsonObject fieldsMap) {
            if (Objects.equals(jpaEntityType.getIdType().getJavaType(), String.class)) {
                return fieldsMap.get("id").getAsString();
            }
            return CommonUtil.getGson().fromJson(fieldsMap, jpaEntityType.getIdType().getJavaType());
        }

        public Object getObject(Map<String, ?> fieldsMap) {
            return CommonUtil.mapModel(fieldsMap, jpaEntityType.getJavaType());
        }

        public Object getObject(JsonObject fieldsMap) {
            return CommonUtil.getGson().fromJson(fieldsMap, jpaEntityType.getJavaType());
        }

        public void updateObject(JsonObject fieldsMap, Object entity) {
            Object updatedRequest = this.getObject(fieldsMap);
            CommonUtil.mapModel(updatedRequest, entity);
        }


    }


}
