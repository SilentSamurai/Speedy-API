package com.github.silent.samurai.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.models.Operator;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.Instant;
import java.util.Date;
import java.util.Iterator;

public class JsonQueryBuilder {

    final CriteriaBuilder criteriaBuilder;
    final CriteriaQuery<?> criteriaQuery;
    final Root<?> root;

    public JsonQueryBuilder(EntityManager entityManager, EntityMetadata entityMetadata) {
        this.criteriaBuilder = entityManager.getCriteriaBuilder();
        this.criteriaQuery = criteriaBuilder.createQuery(entityMetadata.getEntityClass());
        this.root = criteriaQuery.from(entityMetadata.getEntityClass());
    }

    private Predicate[] buildPredicate(ArrayNode arrayNode) throws BadRequestException {
        Iterator<JsonNode> elements = arrayNode.elements();
        Predicate[] predicates = new Predicate[arrayNode.size()];
        int i = 0;
        while (elements.hasNext()) {
            JsonNode jsonNode = elements.next();
            predicates[i++] = buildPredicate(jsonNode);
        }
        return predicates;
    }


    private Predicate buildPredicate(JsonNode node) throws BadRequestException {

        PredicateFactory predicateFactory = new PredicateFactory(criteriaBuilder, root);
        if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            Predicate[] predicates = buildPredicate(arrayNode);
            return criteriaBuilder.and(predicates);
        } else if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            if (objectNode.has("operator") && objectNode.has("field") && objectNode.has("value")) {
                Operator operator = Operator.fromSymbol(objectNode.get("operator").asText());
                String field = objectNode.get("field").asText();
                JsonNode valueNode = objectNode.get("value");
                if (valueNode.isArray()) {
                    return predicateFactory.create(operator, field, valueNode.elements().next().asText());
                } else {
                    return predicateFactory.create(operator, field, valueNode.asText());
                }
            } else if (objectNode.has("operator") && objectNode.has("conditions")) {
                Operator operator = Operator.fromSymbol(objectNode.get("operator").asText());
                JsonNode conditionsNode = objectNode.get("conditions");
                Predicate[] predicates = buildPredicate((ArrayNode) conditionsNode.elements());
                return operator == Operator.AND ? criteriaBuilder.and(predicates) : criteriaBuilder.or(predicates);
            } else if (objectNode.has("operator") && objectNode.has("field") && objectNode.has("value") && objectNode.get("operator").asText().equals("dateBetween")) {
                String field = objectNode.get("field").asText();
                Date start = Date.from(Instant.parse(objectNode.get("value").get("start").asText().replace("Date(", "").replace(")", "")));
                Date end = Date.from(Instant.parse(objectNode.get("value").get("end").asText().replace("Date(", "").replace(")", "")));
                return predicateFactory.createBetween(field, start, end);
            }
        }
        throw new IllegalArgumentException("Invalid JSON input");
    }

}
