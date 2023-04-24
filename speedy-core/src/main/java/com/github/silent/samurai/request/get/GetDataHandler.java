package com.github.silent.samurai.request.get;

import com.github.silent.samurai.exceptions.NotFoundException;
import com.github.silent.samurai.helpers.MetadataUtil;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.FieldMetadata;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class GetDataHandler {

    private final GetRequestContext context;

    public GetDataHandler(GetRequestContext context) {
        this.context = context;
    }

    private Query getQuery(EntityManager entityManager) throws NotFoundException {
        EntityMetadata entityMetadata = context.getResourceMetadata();
        if (entityMetadata == null) {
            throw new RuntimeException("Entity Not Found " + context.getResource());
        }
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<?> cQuery = cb.createQuery(entityMetadata.getEntityClass());
        Root<?> rootObject = cQuery.from(entityMetadata.getEntityClass());
        Predicate[] predicates = null;
        Set<String> keywords = context.getParser().getKeywords();
        if (!keywords.isEmpty()) {
            List<Predicate> dynamicPredicate = new LinkedList<>();
            for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
                if (!context.getParser().hasKeyword(fieldMetadata.getOutputPropertyName())) {
                    continue;
                }
                String name = fieldMetadata.getClassFieldName();
                Object value = context.getParser().getKeyword(fieldMetadata.getOutputPropertyName(), fieldMetadata.getFieldType());
                Predicate equal = cb.equal(rootObject.get(name), value);
                dynamicPredicate.add(equal);
            }
            predicates = dynamicPredicate.toArray(new Predicate[dynamicPredicate.size()]);
        }
        if (predicates != null) {
            cQuery.where(cb.and(predicates));
        }
        return entityManager.createQuery(cQuery);
    }

    public Optional<Object> process() throws Exception {
        Object requestObject;
        if (context.getParser().isOnlyIdentifiersPresent()) {
            EntityMetadata entityMetadata = context.getResourceMetadata();
            Object primaryKeyObject = MetadataUtil.createIdentifierFromParser(context.getParser());
            requestObject = context.getEntityManager().find(entityMetadata.getEntityClass(), primaryKeyObject);
        } else {
            Query query = getQuery(context.getEntityManager());
            requestObject = query.getResultList();
        }
        return Optional.ofNullable(requestObject);
    }
}
