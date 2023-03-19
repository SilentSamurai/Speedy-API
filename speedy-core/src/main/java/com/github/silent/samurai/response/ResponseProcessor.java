package com.github.silent.samurai.response;

import com.github.silent.samurai.helpers.EntityMetadataHelper;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.request.get.GETRequestContext;
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

    public Object getPrimaryKeyObject(GETRequestContext GETRequestContext, EntityMetadata entityMetadata) {
        return EntityMetadataHelper.instance.getPrimaryKey(entityMetadata, GETRequestContext);
    }

    private Query getQuery(EntityManager entityManager, GETRequestContext GETRequestContext) {
        EntityMetadata entityMetadata = GETRequestContext.getResourceMetadata();
        if (entityMetadata == null) {
            throw new RuntimeException("Entity Not Found " + GETRequestContext.getRequest().getResource());
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<?> cQuery = cb.createQuery(entityMetadata.getEntityClass());
        Root<?> rootObject = cQuery.from(entityMetadata.getEntityClass());
        Predicate[] predicates = null;
        if (!GETRequestContext.getKeywords().isEmpty()) {
            int count = 0;
            predicates = new Predicate[GETRequestContext.getKeywords().size()];
            for (Map.Entry<String, String> entry : GETRequestContext.getKeywords().entrySet()) {
                Predicate equal = cb.equal(rootObject.get(entry.getKey()), entry.getValue());
                predicates[count++] = equal;
            }
        }
        if (predicates != null) {
            cQuery.where(cb.and(predicates));
        }
        return entityManager.createQuery(cQuery);
    }

    public JsonElement process(GETRequestContext context, EntityManager entityManager) throws InvocationTargetException, IllegalAccessException {
        JsonElement jsonElement;
        ApiAutomateJsonSerializer apiAutomateJsonSerializer = new ApiAutomateJsonSerializer(metaModelProcessor);
        if (context.getSerializationType() == ApiAutomateJsonSerializer.MULTIPLE_ENTITY) {
            Query query = getQuery(entityManager, context);
            List<?> resultList = query.getResultList();
            jsonElement = apiAutomateJsonSerializer.formCollection(resultList, context.getSerializationType());
        } else {
            EntityMetadata entityMetadata = context.getResourceMetadata();
            Object primaryKeyObject = this.getPrimaryKeyObject(context, entityMetadata);
            Object resultEntity = entityManager.find(entityMetadata.getEntityClass(), primaryKeyObject);
            jsonElement = apiAutomateJsonSerializer.fromObject(resultEntity, resultEntity.getClass(), context.getSerializationType());
        }
        return jsonElement;
    }
}
