package com.github.silent.samurai.request.get;

import com.github.silent.samurai.helpers.MetadataUtil;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.IResponseSerializer;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Map;

public class GetDataHandler {

    private final GetRequestContext context;

    public GetDataHandler(GetRequestContext context) {
        this.context = context;
    }

    private Query getQuery(EntityManager entityManager) {
        EntityMetadata entityMetadata = context.getResourceMetadata();
        if (entityMetadata == null) {
            throw new RuntimeException("Entity Not Found " + context.getRequest().getResource());
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<?> cQuery = cb.createQuery(entityMetadata.getEntityClass());
        Root<?> rootObject = cQuery.from(entityMetadata.getEntityClass());
        Predicate[] predicates = null;
        if (!context.getKeywords().isEmpty()) {
            int count = 0;
            predicates = new Predicate[context.getKeywords().size()];
            for (Map.Entry<String, String> entry : context.getKeywords().entrySet()) {
                Predicate equal = cb.equal(rootObject.get(entry.getKey()), entry.getValue());
                predicates[count++] = equal;
            }
        }
        if (predicates != null) {
            cQuery.where(cb.and(predicates));
        }
        return entityManager.createQuery(cQuery);
    }

    public Object process() throws Exception {
        Object requestObject;
        if (context.getSerializationType() == IResponseSerializer.MULTIPLE_ENTITY) {
            Query query = getQuery(context.getEntityManager());
            requestObject = query.getResultList();
        } else {
            EntityMetadata entityMetadata = context.getResourceMetadata();
            Object primaryKeyObject = MetadataUtil.getPrimaryKey(entityMetadata, context.getKeywords());
            requestObject = context.getEntityManager().find(entityMetadata.getEntityClass(), primaryKeyObject);
        }
        return requestObject;
    }
}
