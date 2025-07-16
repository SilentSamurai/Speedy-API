package com.github.silent.samurai.speedy.query;

import com.github.silent.samurai.speedy.helpers.MetadataUtil;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.*;
import com.github.silent.samurai.speedy.models.conditions.EqCondition;
import com.github.silent.samurai.speedy.utils.SpeedyValueFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SpeedyQueryHelper {

    public final Map<String, BinaryCondition> conditionMap = new HashMap<>();
    private final SpeedyQuery speedyQuery;

    public SpeedyQueryHelper(SpeedyQuery speedyQuery) {
        this.speedyQuery = speedyQuery;
        this.initial();
    }

    private void initial() {
        speedyQuery.getWhere()
                .getConditions()
                .stream()
                .filter(BinaryCondition.class::isInstance)
                .map(BinaryCondition.class::cast)
                .forEach(binaryCondition ->
                        conditionMap.put(binaryCondition.getField().getFieldMetadata().getOutputPropertyName(), binaryCondition));
    }

    public boolean isFilterPresent(FieldMetadata fieldMetadata) {
        return conditionMap.containsKey(fieldMetadata.getOutputPropertyName());
    }

    public Optional<BinaryCondition> getCondition(FieldMetadata fieldMetadata) {
        if (isFilterPresent(fieldMetadata)) {
            return Optional.of(conditionMap.get(fieldMetadata.getOutputPropertyName()));
        }
        return Optional.empty();
    }

    public Optional<Expression> getFilterValue(FieldMetadata fieldMetadata) {
        if (isFilterPresent(fieldMetadata)) {
            return Optional.of(conditionMap.get(fieldMetadata.getOutputPropertyName()).getExpression());
        }
        return Optional.empty();
    }

    public boolean isOnlyIdentifiersPresent() {
        boolean isAllEqualCondition = speedyQuery.getWhere().getConditions()
                .stream()
                .allMatch(EqCondition.class::isInstance);
        if (isAllEqualCondition) {
            Set<String> keywords = speedyQuery.getWhere().getConditions().stream()
                    .map(EqCondition.class::cast)
                    .map(condition -> condition.getField().getFieldMetadata().getOutputPropertyName())
                    .collect(Collectors.toSet());
            return MetadataUtil.hasOnlyPrimaryKeyFields(speedyQuery.getFrom(), keywords);
        }
        return false;
    }

    public boolean isIdentifiersPresent() {
        EntityMetadata entityMetadata = speedyQuery.getFrom();
        return entityMetadata.getKeyFields().stream().allMatch(this::isFilterPresent);
    }

    public <T> T rawValueFromCondition(FieldMetadata fieldMetadata, Class<T> clazz) throws Exception {
        Expression filterValue = getFilterValue(fieldMetadata).orElseThrow();
        if (filterValue instanceof Literal literal) {
            return SpeedyValueFactory.toJavaType(fieldMetadata, literal.value());
        } else if (filterValue instanceof Identifier identifier) {
            return (T) identifier.field().getFieldMetadata().getDbColumnName();
        }
        throw new Exception("invalid filter value for field: " + fieldMetadata.getOutputPropertyName());
    }

}
