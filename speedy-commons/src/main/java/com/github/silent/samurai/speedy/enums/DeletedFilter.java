package com.github.silent.samurai.speedy.enums;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;

/// Controls how soft-deleted rows are treated by a read request ({@code $deleted} query param).
///
/// - {@link #EXCLUDE} (default): hide soft-deleted rows.
/// - {@link #INCLUDE}: return live and soft-deleted rows.
/// - {@link #ONLY}: return only soft-deleted rows (a recycle-bin view).
///
/// {@code INCLUDE}/{@code ONLY} are gated per entity by {@code @SpeedySoftDelete(allowViewDeleted=true)}.
public enum DeletedFilter {
    EXCLUDE, INCLUDE, ONLY;

    public static DeletedFilter fromString(String value) throws BadRequestException {
        if (value == null) {
            return EXCLUDE;
        }
        return switch (value.trim().toLowerCase()) {
            case "", "exclude" -> EXCLUDE;
            case "include" -> INCLUDE;
            case "only" -> ONLY;
            default -> throw new BadRequestException(
                    "Invalid value for $deleted: '" + value + "'. Expected exclude, include, or only.");
        };
    }
}
