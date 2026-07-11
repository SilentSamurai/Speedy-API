package com.github.silent.samurai.speedy.annotations;

import com.github.silent.samurai.speedy.enums.BulkOperation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Opts an entity into bulk (multi-element) request bodies for a chosen set of write operations.
///
/// Without this annotation an entity rejects any multi-element create/update/replace/delete body
/// with 400; single-object and single-element-array bodies always work. Declaring the annotation
/// enables bulk only for the listed {@link BulkOperation}s:
///
/// - `@SpeedyBulk` (or `@SpeedyBulk(BulkOperation.ALL)`) — bulk on every write operation.
/// - `@SpeedyBulk(BulkOperation.CREATE)` — bulk create only; bulk update/replace/delete still rejected.
/// - `@SpeedyBulk({BulkOperation.CREATE, BulkOperation.DELETE})` — bulk on those two only.
/// - `@SpeedyBulk({})` — explicitly no bulk (equivalent to omitting the annotation).
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SpeedyBulk {
    BulkOperation[] value() default {BulkOperation.ALL};
}
