package com.github.silent.samurai.speedy.policy.model;

import lombok.Getter;

import java.util.Objects;

/// Parses and matches a policy resource token, e.g. {@code "Employee.salary"} (a single field)
/// or {@code "Employee.*"} (every field of the entity). Entity-level decisions (CREATE/DELETE,
/// or "does this caller have any READ access at all") match on entity name alone via
/// {@link #matchesEntity}, regardless of whether the selector is field-specific or wildcard.
@Getter
public final class ResourceSelector {

    private final String entity;
    private final String field;
    private final boolean wholeEntity;

    private ResourceSelector(String entity, String field, boolean wholeEntity) {
        this.entity = entity;
        this.field = field;
        this.wholeEntity = wholeEntity;
    }

    public static ResourceSelector parse(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Resource selector cannot be blank");
        }
        int dot = token.indexOf('.');
        if (dot < 0) {
            return new ResourceSelector(token, null, true);
        }
        if (dot == token.length() - 1) {
            throw new IllegalArgumentException(
                    "Resource selector '" + token + "' must be an entity or of the form 'Entity.field' or 'Entity.*'");
        }
        String entity = token.substring(0, dot);
        String rest = token.substring(dot + 1);
        if ("*".equals(rest)) {
            return new ResourceSelector(entity, null, true);
        }
        return new ResourceSelector(entity, rest, false);
    }

    public boolean matchesEntity(String entityName) {
        return entity.equals(entityName);
    }

    public boolean matches(String entityName, String fieldName) {
        if (!matchesEntity(entityName)) {
            return false;
        }
        return wholeEntity || field.equals(fieldName);
    }

    @Override
    public String toString() {
        return entity + "." + (wholeEntity ? "*" : field);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceSelector that)) return false;
        return wholeEntity == that.wholeEntity
                && Objects.equals(entity, that.entity)
                && Objects.equals(field, that.field);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity, field, wholeEntity);
    }
}
