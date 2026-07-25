package com.github.silent.samurai.speedy.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Declares a scalar field as a Speedy association without a native JPA relationship
/// (`@ManyToOne`/`@OneToOne`) backing it — e.g. a plain `UUID` column that logically
/// references another entity's primary key but can't be remapped to an object reference.
/// Always binds to the target entity's primary key, mirroring how `@ManyToOne` is resolved.
///
/// The target entity is given as exactly one of {@link #value()} (its JPA entity class) or
/// {@link #entity()} (its Speedy entity name) — use the latter when the target class isn't
/// available to reference directly, e.g. across module boundaries.
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SpeedyAssociation {

    /// The target JPA entity class. Mutually exclusive with {@link #entity()}.
    Class<?> value() default Void.class;

    /// The target Speedy entity name. Mutually exclusive with {@link #value()}.
    String entity() default "";
}
