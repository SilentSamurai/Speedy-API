package com.github.silent.samurai.speedy.utils;

import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.models.SpeedyEntity;

import java.util.Arrays;
import java.util.Optional;

/// Computes the weak {@code ETag} for an entity from its declared version field
/// ({@link com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata#getVersionField()}),
/// and evaluates {@code If-Match}/{@code If-None-Match} header values against it. Entities with no
/// version field have no ETag. Speedy only ever generates weak validators, so both headers are
/// compared weakly (the {@code W/} prefix and surrounding quotes are stripped before comparison).
public final class EtagUtil {

    private EtagUtil() {
    }

    /// The entity's current ETag, or empty if it declares no version field or the field is
    /// currently unset.
    public static Optional<String> computeEtag(SpeedyEntity entity) {
        Optional<FieldMetadata> versionField = entity.getMetadata().getVersionField();
        if (versionField.isEmpty()) {
            return Optional.empty();
        }
        FieldMetadata field = versionField.get();
        if (!entity.has(field)) {
            return Optional.empty();
        }
        SpeedyValue value = entity.get(field);
        if (value == null || value.isNull() || value.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of("W/\"" + stringify(value) + "\"");
    }

    private static String stringify(SpeedyValue value) {
        return switch (value.getValueType()) {
            case TEXT -> value.asText();
            case INT -> String.valueOf(value.asInt());
            case FLOAT -> String.valueOf(value.asDouble());
            case DATE -> value.asDate().toString();
            case TIME -> value.asTime().toString();
            case DATE_TIME -> value.asDateTime().toString();
            case ZONED_DATE_TIME -> value.asZonedDateTime().toString();
            case ENUM -> value.asEnum();
            case ENUM_ORD -> String.valueOf(value.asEnumOrd());
            case BOOL -> String.valueOf(value.asBoolean());
            case NULL, OBJECT, COLLECTION -> value.toString();
        };
    }

    /// Whether the raw (possibly comma-separated) {@code If-Match} header value is satisfied by
    /// {@code currentEtag} ({@code null} when the entity has no version field, or no row exists —
    /// callers pass whichever is appropriate). {@code *} matches any existing resource.
    public static boolean ifMatchSatisfied(String ifMatchHeader, String currentEtag) {
        return matchesAny(ifMatchHeader, currentEtag);
    }

    /// Whether the raw (possibly comma-separated) {@code If-None-Match} header value matches
    /// {@code currentEtag} — i.e. the client's cached copy is still fresh. {@code *} matches any
    /// existing resource.
    public static boolean ifNoneMatchSatisfied(String ifNoneMatchHeader, String currentEtag) {
        return matchesAny(ifNoneMatchHeader, currentEtag);
    }

    private static boolean matchesAny(String headerValue, String currentEtag) {
        if (currentEtag == null) {
            return false;
        }
        if ("*".equals(headerValue.trim())) {
            return true;
        }
        String normalizedCurrent = normalize(currentEtag);
        return Arrays.stream(headerValue.split(","))
                .map(EtagUtil::normalize)
                .anyMatch(candidate -> candidate.equals(normalizedCurrent));
    }

    /// Strips the {@code W/} weak-validator prefix and surrounding quotes for comparison.
    private static String normalize(String tag) {
        String trimmed = tag.trim();
        if (trimmed.regionMatches(true, 0, "W/", 0, 2)) {
            trimmed = trimmed.substring(2).trim();
        }
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}
