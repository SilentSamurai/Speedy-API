package com.github.silent.samurai.speedy.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Opts an entity into soft delete. When present, a {@code DELETE /{Entity}/$delete} sets the
/// named marker field instead of removing the row, and soft-deleted rows are hidden from
/// {@code GET}/{@code $query} by default.
///
/// The marker {@link #field()} must be a boolean or a temporal field (BOOL, DATE, DATE_TIME,
/// ZONED_DATE_TIME); the engine sets a timestamp/{@code true} on delete and clears it on restore.
///
/// Both view and hard-delete are secure by default (off):
/// - {@link #allowViewDeleted()} gates {@code $deleted=include|only} read requests.
/// - {@link #allowHardDelete()} gates the {@code POST /{Entity}/$purge} permanent-delete endpoint.
///
/// The {@code POST /{Entity}/$restore} endpoint is available whenever soft delete is enabled.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SpeedySoftDelete {

    /// The marker field set on delete and cleared on restore (must be BOOL or a temporal type).
    String field();

    /// Whether {@code $deleted=include|only} read requests are accepted. Off by default so
    /// soft-deleted rows stay hidden unless the entity explicitly allows viewing them.
    boolean allowViewDeleted() default false;

    /// Whether the {@code POST /{Entity}/$purge} permanent hard-delete endpoint is exposed. Off by
    /// default so soft-deleted data cannot be irreversibly destroyed unless explicitly enabled.
    boolean allowHardDelete() default false;
}
