package com.github.silent.samurai.speedy.policy;

import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;

/// The concrete (entity, field) pair a single call is asking
/// about — wildcard-free, unlike a rule's {@code "Entity"} / {@code "Entity.field"} / {@code "Entity.*"}
/// selector pattern, which this is matched against. {@code field} is {@code null} for an entity-level check.
public record PolicyTarget(EntityMetadata entity, FieldMetadata field) {

    public static PolicyTarget entity(EntityMetadata entity) {
        return new PolicyTarget(entity, null);
    }

    public static PolicyTarget field(EntityMetadata entity, FieldMetadata field) {
        return new PolicyTarget(entity, field);
    }
}
