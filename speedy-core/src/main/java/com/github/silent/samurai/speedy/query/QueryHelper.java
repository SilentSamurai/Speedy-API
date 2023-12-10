package com.github.silent.samurai.speedy.query;

import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class QueryHelper {

    public final Map<FieldMetadata, BinaryCondition> conditionMap = new HashMap<>();
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
                        conditionMap.put(binaryCondition.getField().getFieldMetadata(), binaryCondition));
    }

    public boolean isFilterPresent(FieldMetadata fieldMetadata) {
        return conditionMap.containsKey(fieldMetadata);
    }

    public Optional<SpeedyValue> getFilterValue(FieldMetadata fieldMetadata) {
        if (isFilterPresent(fieldMetadata)) {
            return Optional.of(conditionMap.get(fieldMetadata).getSpeedyValue());
        }
        return Optional.empty();
    }

}
