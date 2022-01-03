package com.github.silent.samurai.response;

import com.github.silent.samurai.metamodel.JpaMetaModel;
import com.github.silent.samurai.metamodel.RequestInfo;
import com.github.silent.samurai.metamodel.ResourceMetadata;
import com.github.silent.samurai.serializers.ApiAutomateJsonSerializer;
import com.google.gson.JsonElement;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Attribute;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public class ResponseProcessor {

    JpaMetaModel jpaMetaModel;

    public ResponseProcessor(JpaMetaModel jpaMetaModel) {
        this.jpaMetaModel = jpaMetaModel;
    }

    public Attribute<?, ?> getJpaAttribute(ResourceMetadata entityMetadata, String fieldName) {
        return entityMetadata.getJpaEntityType().getAttribute(fieldName);
    }

    public Object getPrimaryKeyObject(RequestInfo requestInfo, ResourceMetadata entityMetadata) {
        return entityMetadata.getPrimaryKeyObject(requestInfo.filters);
    }

    private Query getQuery(EntityManager entityManager, RequestInfo requestInfo) {
        ResourceMetadata entityMetadata = jpaMetaModel.getEntityMetadata(requestInfo.resourceType);
        if (entityMetadata == null) {
            throw new RuntimeException("Entity Not Found " + requestInfo.resourceType);
        }
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<?> cQuery = cb.createQuery(entityMetadata.getJpaEntityType().getJavaType());
        Root<?> rootObject = cQuery.from(entityMetadata.getJpaEntityType().getJavaType());
        Predicate[] predicates = null;
        if (requestInfo.filters != null && !requestInfo.filters.isEmpty()) {
            int count = 0;
            predicates = new Predicate[requestInfo.filters.size()];
            for (Map.Entry<String, String> entry : requestInfo.filters.entrySet()) {
                Predicate equal = cb.equal(rootObject.get(entry.getKey()), entry.getValue());
                predicates[count++] = equal;
            }
        }
        if (predicates != null) {
            cQuery.where(cb.and(predicates));
        }
        return entityManager.createQuery(cQuery);
    }

    public JsonElement process(RequestInfo requestInfo, EntityManager entityManager) throws InvocationTargetException, IllegalAccessException {
        JsonElement jsonElement;
        ApiAutomateJsonSerializer apiAutomateJsonSerializer = new ApiAutomateJsonSerializer(jpaMetaModel);
        if (requestInfo.serializationType == ApiAutomateJsonSerializer.MULTIPLE_ENTITY) {
            Query query = getQuery(entityManager, requestInfo);
            List<?> resultList = query.getResultList();
            jsonElement = apiAutomateJsonSerializer.formCollection(resultList, requestInfo.serializationType);
        } else {
            ResourceMetadata entityMetadata = jpaMetaModel.getEntityMetadata(requestInfo.resourceType);
            Object primaryKeyObject = this.getPrimaryKeyObject(requestInfo, entityMetadata);
            Object resultEntity = entityManager.find(entityMetadata.getJpaEntityType().getJavaType(), primaryKeyObject);
            jsonElement = apiAutomateJsonSerializer.fromObject(resultEntity, resultEntity.getClass(), requestInfo.serializationType);
        }
        return jsonElement;
    }
}
