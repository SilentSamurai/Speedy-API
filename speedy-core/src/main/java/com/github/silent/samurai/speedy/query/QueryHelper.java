package com.github.silent.samurai.speedy.query;

import com.github.silent.samurai.speedy.helpers.MetadataUtil;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyValueFactory;
import com.github.silent.samurai.speedy.models.conditions.EqCondition;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class QueryHelper {

    public final Map<String, BinaryCondition> conditionMap = new HashMap<>();
    private final SpeedyQuery speedyQuery;

    public QueryHelper(SpeedyQuery speedyQuery) {
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

    public Optional<SpeedyValue> getFilterValue(FieldMetadata fieldMetadata) {
        if (isFilterPresent(fieldMetadata)) {
            return Optional.of(conditionMap.get(fieldMetadata.getOutputPropertyName()).getSpeedyValue());
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
                    .map(condition -> condition.getField().getFieldMetadata().getClassFieldName())
                    .collect(Collectors.toSet());
            return MetadataUtil.hasOnlyPrimaryKeyFields(speedyQuery.getFrom(), keywords);
        }
        return false;
    }

    public boolean isIdentifiersPresent() {
        EntityMetadata entityMetadata = speedyQuery.getFrom();
        return entityMetadata.getKeyFields().stream().allMatch(this::isFilterPresent);
    }

    public <T> T getRawValueOfValue(FieldMetadata fieldMetadata, Class<T> clazz) throws Exception {
        Optional<SpeedyValue> filterValue = getFilterValue(fieldMetadata);
        return SpeedyValueFactory.speedyValueToJavaType(filterValue.get(), clazz);
    }

}
