package com.github.silent.samurai.speedy.serializers;

import com.github.silent.samurai.speedy.interfaces.FieldMetadata;

import java.util.Set;
import java.util.function.Predicate;

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
