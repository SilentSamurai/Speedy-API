package com.github.silent.samurai.speedy.policy.condition;

import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.models.SpeedyEntity;

import java.util.Map;

/// The evaluation context a {@link PolicyCondition} is checked against: the row being decided
/// (nullable — absent at query-parse time, before any row has been fetched), pre-resolved
/// variables for {@code ${name}} references, and the entity metadata the row belongs to.
public record ConditionContext(SpeedyEntity row, Map<String, SpeedyValue> variables, EntityMetadata entityMetadata) {

    public ConditionContext(SpeedyEntity row, EntityMetadata entityMetadata) {
        this(row, null, entityMetadata);
    }

    public boolean hasRow() {
        return row != null;
    }

}
