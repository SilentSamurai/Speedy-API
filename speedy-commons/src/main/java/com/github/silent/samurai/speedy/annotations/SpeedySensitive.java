package com.github.silent.samurai.speedy.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SpeedySensitive {

    /**
     * When {@code true} (default), the annotated field or all fields on
     * the annotated entity cannot be used as {@code $} field references
     * in query conditions. Field-level annotations override entity-level.
     *
     * <pre>{@code
     * // Field-level: blocks ?otherField=$secretField
     * @SpeedySensitive
     * private String secretField;
     *
     * // Entity-level: all fields are sensitive unless individually
     * // overridden with @SpeedySensitive(false)
     * @SpeedySensitive
     * @Entity
     * public class MyEntity { ... }
     * }</pre>
     */
    boolean value() default true;
}
