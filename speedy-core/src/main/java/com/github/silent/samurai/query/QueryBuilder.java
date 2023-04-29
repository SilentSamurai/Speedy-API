package com.github.silent.samurai.query;

import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.SpeedyConstant;
import com.github.silent.samurai.models.Operator;
import com.github.silent.samurai.models.conditions.Condition;
import com.github.silent.samurai.parser.SpeedyUriParser;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.*;
import java.util.stream.Collectors;

public class QueryBuilder {

    private final SpeedyUriParser parser;
    private final EntityMetadata entityMetadata;
    private final EntityManager entityManager;
    private final CriteriaBuilder criteriaBuilder;
    private final CriteriaQuery<?> query;
    private final Root<?> tableRoot;

    public QueryBuilder(SpeedyUriParser parser, EntityMetadata entityMetadata, EntityManager entityManager) {
        this.parser = parser;
        this.entityMetadata = entityMetadata;
        this.entityManager = entityManager;
        Objects.requireNonNull(entityMetadata, "Entity Not Found " + parser.getResource());
        Objects.requireNonNull(parser, "query required");
        criteriaBuilder = entityManager.getCriteriaBuilder();
        query = criteriaBuilder.createQuery(entityMetadata.getEntityClass());
        tableRoot = query.from(entityMetadata.getEntityClass());
    }

    private void addWhereQuery() throws Exception {
        List<String> conditions = parser.getConditionChain();
        if (conditions.isEmpty()) {
            return;
        }
        Iterator<String> iterator = conditions.iterator();
        Condition condition = parser.getConditionByInternalId(iterator.next());
        Predicate predicate = condition.getPredicate(criteriaBuilder, tableRoot, entityMetadata);
        while (iterator.hasNext()) {
            String operator = iterator.next();
            String internalId = iterator.next();
            Condition nextCondition = parser.getConditionByInternalId(internalId);
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
        query.where(predicate);
    }

    private void addToOrderList(List<Order> orderList, String queryName, boolean isDesc) throws Exception {
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
                if (isDesc) {
                    orderList.add(criteriaBuilder.desc(tableRoot.get(orderBy)));
                } else {
                    orderList.add(criteriaBuilder.asc(tableRoot.get(orderBy)));
                }
            }
        }
    }

    private void addOrderBy() throws Exception {
        List<Order> orderList = new LinkedList<>();
        addToOrderList(orderList, "orderBy", false);
        addToOrderList(orderList, "orderByDesc", true);
        query.orderBy(orderList);
    }

    private TypedQuery<?> addPageInfo() throws BadRequestException {
        TypedQuery<?> paggedQuery = entityManager.createQuery(query);
        int pageSize = parser.getQueryOrDefault("pageSize", Integer.class, SpeedyConstant.defaultPageSize);
        int pageNumber = parser.getQueryOrDefault("pageNo", Integer.class, 0);
        paggedQuery.setMaxResults(pageSize);
        paggedQuery.setFirstResult(pageSize * pageNumber);
        return paggedQuery;
    }

    public Query getQuery() throws Exception {
        if (!parser.getConditionChain().isEmpty()) {
            addWhereQuery();
        }
        addOrderBy();
        return addPageInfo();
    }


}
