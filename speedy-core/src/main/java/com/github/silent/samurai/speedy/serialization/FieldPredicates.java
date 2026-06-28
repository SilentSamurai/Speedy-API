package com.github.silent.samurai.speedy.serialization;

import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;

import java.util.Set;
import java.util.function.Predicate;

/// # FieldPredicates
///
/// Utility for building a field-level filter predicate from the {@code $select}
/// clause. Translates a {@code Set<String>} of output property names into a
/// {@code Predicate<FieldMetadata>} for use by
/// {@link SpeedyToStructure}.
///
/// ## Purpose
/// - Builds a predicate that returns {@code true} only for fields in the select set
/// - Returns a pass-through predicate when select is null or empty
public final class FieldPredicates {

    private FieldPredicates() {
    }

    public static Predicate<FieldMetadata> buildFieldPredicate(Set<String> select) {
        if (select == null || select.isEmpty()) {
            return fieldMetadata -> true;
        }
        return fieldMetadata -> select.contains(fieldMetadata.getOutputPropertyName());
    }
}
