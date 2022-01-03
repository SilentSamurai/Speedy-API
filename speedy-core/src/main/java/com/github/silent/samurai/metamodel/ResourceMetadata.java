package com.github.silent.samurai.metamodel;

import com.github.silent.samurai.enums.IgnoreType;
import com.github.silent.samurai.utils.CommonUtil;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;

import javax.persistence.metamodel.EntityType;
import java.util.*;

public class ResourceMetadata {

    private String name;
    private EntityType<?> jpaEntityType;
    private Set<MemberMetadata> membersMetadata = new HashSet<>();
    private Map<String, MemberMetadata> memberMap = new HashMap<>();
    private Set<String> idFields = new HashSet<>();


    public boolean isKeyField(String field) {
        return memberMap.get(field).isId();
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
        JsonObject jsonObject = fieldsMap;
        for (MemberMetadata memberMetadata : this.getMembersMetadata()) {
            if (memberMetadata.getIgnoreType() != null && memberMetadata.getIgnoreType() == IgnoreType.PERSIST) {
                jsonObject.remove(memberMetadata.getName());
            }
        }
        return CommonUtil.getGson().fromJson(jsonObject, jpaEntityType.getJavaType());
    }

    public void updateObject(JsonObject fieldsMap, Object entity) {
        Object updatedRequest = this.getObject(fieldsMap);
        CommonUtil.mapModel(updatedRequest, entity);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EntityType<?> getJpaEntityType() {
        return jpaEntityType;
    }

    public void setJpaEntityType(EntityType<?> jpaEntityType) {
        this.jpaEntityType = jpaEntityType;
    }

    public Set<MemberMetadata> getMembersMetadata() {
        return membersMetadata;
    }

    public void setMembersMetadata(Set<MemberMetadata> membersMetadata) {
        this.membersMetadata = membersMetadata;
    }

    public Map<String, MemberMetadata> getMemberMap() {
        return memberMap;
    }

    public void setMemberMap(Map<String, MemberMetadata> memberMap) {
        this.memberMap = memberMap;
    }

    public Set<String> getIdFields() {
        return idFields;
    }

    public void setIdFields(Set<String> idFields) {
        this.idFields = idFields;
    }
}
