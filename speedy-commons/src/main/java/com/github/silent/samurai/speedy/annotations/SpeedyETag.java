package com.github.silent.samurai.speedy.annotations;

import com.github.silent.samurai.speedy.enums.EtagStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Opts an entity into HTTP conditional requests (ETag / If-Match / If-None-Match) by
 * designating the annotated field as the version token. Speedy owns the field's value: it
 * is stamped with a fresh value on create and every update/replace (per {@link #strategy()}),
 * then read back — never hashed, never client-supplied — to form the {@code ETag}.
 * <p>
 * Entities with no {@code @SpeedyETag} field (and no JPA {@code @Version} field of a
 * compatible type) never emit an {@code ETag} and never honor {@code If-None-Match}; a
 * write carrying {@code If-Match} against such an entity is rejected with {@code 400}.
 *
 * <pre>{@code
 * @SpeedyETag
 * private String rowVersion;
 *
 * @SpeedyETag(strategy = EtagStrategy.TIMESTAMP)
 * private LocalDateTime updatedAt;
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SpeedyETag {

    EtagStrategy strategy() default EtagStrategy.RANDOM;

}
