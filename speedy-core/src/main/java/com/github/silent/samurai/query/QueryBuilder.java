package com.github.silent.samurai.query;

import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.FieldMetadata;
import com.github.silent.samurai.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.interfaces.SpeedyConstant;
import com.github.silent.samurai.models.Operator;
import com.github.silent.samurai.models.conditions.Condition;
import com.github.silent.samurai.parser.ResourceSelector;
import com.github.silent.samurai.parser.SpeedyUriContext;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.*;
import java.util.stream.Collectors;

public class QueryBuilder {

    private final ResourceSelector resourceSelector;
    private final EntityMetadata entityMetadata;
    private final EntityManager entityManager;
    private final CriteriaBuilder criteriaBuilder;
    private final CriteriaQuery<?> query;
    private final Root<?> tableRoot;
    private Predicate previousWherePredicate;


    public QueryBuilder(ResourceSelector resourceSelector, EntityManager entityManager) {
        this.resourceSelector = resourceSelector;
        this.entityMetadata = resourceSelector.getResourceMetadata();
        this.entityManager = entityManager;
        Objects.requireNonNull(entityMetadata, "Entity Not Found " + resourceSelector.getResource());
        Objects.requireNonNull(resourceSelector, "query required");
        criteriaBuilder = entityManager.getCriteriaBuilder();
        query = criteriaBuilder.createQuery(entityMetadata.getEntityClass());
        tableRoot = query.from(entityMetadata.getEntityClass());
    }

    public void addWhereQuery() throws Exception {
        List<String> conditions = resourceSelector.getConditionChain();
        if (conditions.isEmpty()) {
            return;
        }
        Iterator<String> iterator = conditions.iterator();
        Condition condition = resourceSelector.getConditionByInternalId(iterator.next());
        Predicate predicate = condition.getPredicate(criteriaBuilder, tableRoot, entityMetadata);
        while (iterator.hasNext()) {
            String operator = iterator.next();
            String internalId = iterator.next();
            Condition nextCondition = resourceSelector.getConditionByInternalId(internalId);
            Predicate currentPredicate = nextCondition.getPredicate(criteriaBuilder, tableRoot, entityMetadata);
            if (Operator.fromSymbol(operator) == Operator.AND) {
                currentPredicate = criteriaBuilder.and(predicate, currentPredicate);
            } else if (Operator.fromSymbol(operator) == Operator.OR) {
                currentPredicate = criteriaBuilder.or(predicate, currentPredicate);
            } else {
                throw new BadRequestException();
            }
            predicate = currentPredicate;
        }
        previousWherePredicate = predicate;
    }

    public void addAssociationFK(EntityMetadata secondaryResource, Object instance) throws BadRequestException {
        List<Predicate> fkPredicate = new LinkedList<>();
        FieldMetadata associatedField = this.entityMetadata.getAssociatedField(secondaryResource)
                .orElseThrow(BadRequestException::new);
        Path<Object> objectPath = tableRoot.get(associatedField.getClassFieldName());
        for (KeyFieldMetadata keyField : secondaryResource.getKeyFields()) {
            Object fieldValue = keyField.getEntityFieldValue(instance);
            Predicate equal = criteriaBuilder.equal(objectPath.get(keyField.getClassFieldName()), fieldValue);
            fkPredicate.add(equal);
        }
        if (previousWherePredicate != null) {
            fkPredicate.add(previousWherePredicate);
        }
        Predicate[] predicates = fkPredicate.toArray(new Predicate[fkPredicate.size()]);
        previousWherePredicate = criteriaBuilder.and(predicates);
    }


    private void addToOrderList(SpeedyUriContext parser,
                                List<Order> orderList,
                                String queryName,
                                boolean isDesc) throws Exception {
        if (parser.hasQuery(queryName)) {
            Map<Boolean, List<String>> collect = parser.getQuery(queryName, String.class)
                    .stream()
                    .collect(Collectors.partitioningBy(qry -> qry.contains(",")));
            List<String> withComma = collect.get(true);
            List<String> withoutComma = collect.get(false);
            withComma.stream()
                    .flatMap(qry -> Arrays.stream(qry.split(",")))
                    .forEach(withoutComma::add);
            for (String orderBy : withoutComma) {
                if (!this.entityMetadata.has(orderBy)) {
                    throw new BadRequestException(orderBy + " field not found");
                }
                if (isDesc) {
                    orderList.add(criteriaBuilder.desc(tableRoot.get(orderBy)));
                } else {
                    orderList.add(criteriaBuilder.asc(tableRoot.get(orderBy)));
                }
            }
        }
    }

    private void addOrderBy(SpeedyUriContext parser) throws Exception {
        List<Order> orderList = new LinkedList<>();
        addToOrderList(parser, orderList, "orderBy", false);
        addToOrderList(parser, orderList, "orderByDesc", true);
        query.orderBy(orderList);
    }

    private TypedQuery<?> addPageInfo(SpeedyUriContext parser) throws BadRequestException {
        TypedQuery<?> paggedQuery = entityManager.createQuery(query);
        int pageSize = parser.getQueryOrDefault("pageSize", Integer.class, SpeedyConstant.defaultPageSize);
        int pageNumber = parser.getQueryOrDefault("pageNo", Integer.class, 0);
        paggedQuery.setMaxResults(pageSize);
        paggedQuery.setFirstResult(pageSize * pageNumber);
        return paggedQuery;
    }

    public Query getQuery(SpeedyUriContext parser) throws Exception {
        if (previousWherePredicate != null) {
            query.where(previousWherePredicate);
        }
        addOrderBy(parser);
        return addPageInfo(parser);
    }


}
