package com.github.silent.samurai.speedy.policy.condition;

import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.util.Objects;

/// Compares two {@link SpeedyValue}s for a policy data condition (e.g. {@code FieldEquals}).
/// Same-type comparisons use the typed accessor; cross-type comparisons (e.g. a principal id
/// delivered as text vs. a numeric key column) fall back to a textual comparison rather than
/// failing closed on a type mismatch alone.
public final class PolicyValueEquals {

    private PolicyValueEquals() {
    }

    public static boolean equals(SpeedyValue a, SpeedyValue b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.isNull() || b.isNull()) {
            return a.isNull() && b.isNull();
        }
        if (a.getValueType() == b.getValueType()) {
            return typedEquals(a, b);
        }
        return textOf(a).equals(textOf(b));
    }

    private static boolean typedEquals(SpeedyValue a, SpeedyValue b) {
        return switch (a.getValueType()) {
            case TEXT, ENUM -> Objects.equals(a.asText(), b.asText());
            case INT, ENUM_ORD -> Objects.equals(a.asInt(), b.asInt());
            case FLOAT -> Objects.equals(a.asDouble(), b.asDouble());
            case BOOL -> Objects.equals(a.asBoolean(), b.asBoolean());
            case DATE -> Objects.equals(a.asDate(), b.asDate());
            case DATE_TIME -> Objects.equals(a.asDateTime(), b.asDateTime());
            case TIME -> Objects.equals(a.asTime(), b.asTime());
            case ZONED_DATE_TIME -> Objects.equals(a.asZonedDateTime(), b.asZonedDateTime());
            case NULL -> true;
            case OBJECT, COLLECTION -> Objects.equals(a, b);
        };
    }

    private static String textOf(SpeedyValue v) {
        return switch (v.getValueType()) {
            case TEXT, ENUM -> v.asText();
            case INT, ENUM_ORD -> String.valueOf(v.asInt());
            case FLOAT -> String.valueOf(v.asDouble());
            case BOOL -> String.valueOf(v.asBoolean());
            case DATE -> String.valueOf(v.asDate());
            case DATE_TIME -> String.valueOf(v.asDateTime());
            case TIME -> String.valueOf(v.asTime());
            case ZONED_DATE_TIME -> String.valueOf(v.asZonedDateTime());
            case NULL -> "";
            case OBJECT, COLLECTION -> String.valueOf(v);
        };
    }
}
