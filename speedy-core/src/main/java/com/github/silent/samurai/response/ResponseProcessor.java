package com.github.silent.samurai.response;

import com.github.silent.samurai.helpers.EntityMetadataHelper;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.metamodel.JpaEntityMetadata;
import com.github.silent.samurai.metamodel.JpaMetaModelProcessor;
import com.github.silent.samurai.metamodel.RequestInfo;
import com.github.silent.samurai.serializers.ApiAutomateJsonSerializer;
import com.google.gson.JsonElement;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public class ResponseProcessor {

    MetaModelProcessor metaModelProcessor;

    public ResponseProcessor(MetaModelProcessor metaModelProcessor) {
        this.metaModelProcessor = metaModelProcessor;
    }

    public Object getPrimaryKeyObject(RequestInfo requestInfo, EntityMetadata entityMetadata) {
        return EntityMetadataHelper.instance.getPrimaryKey(entityMetadata, requestInfo);
    }

    private Query getQuery(EntityManager entityManager, RequestInfo requestInfo) {
        EntityMetadata entityMetadata = metaModelProcessor.findEntityMetadata(requestInfo.resourceType);
        if (entityMetadata == null) {
            throw new RuntimeException("Entity Not Found " + requestInfo.resourceType);
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<?> cQuery = cb.createQuery(entityMetadata.getEntityClass());
        Root<?> rootObject = cQuery.from(entityMetadata.getEntityClass());
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
        ApiAutomateJsonSerializer apiAutomateJsonSerializer = new ApiAutomateJsonSerializer(metaModelProcessor);
        if (requestInfo.serializationType == ApiAutomateJsonSerializer.MULTIPLE_ENTITY) {
            Query query = getQuery(entityManager, requestInfo);
            List<?> resultList = query.getResultList();
            jsonElement = apiAutomateJsonSerializer.formCollection(resultList, requestInfo.serializationType);
        } else {
            EntityMetadata entityMetadata = metaModelProcessor.findEntityMetadata(requestInfo.resourceType);
            Object primaryKeyObject = this.getPrimaryKeyObject(requestInfo, entityMetadata);
            Object resultEntity = entityManager.find(entityMetadata.getEntityClass(), primaryKeyObject);
            jsonElement = apiAutomateJsonSerializer.fromObject(resultEntity, resultEntity.getClass(), requestInfo.serializationType);
        }
        return jsonElement;
    }
}
