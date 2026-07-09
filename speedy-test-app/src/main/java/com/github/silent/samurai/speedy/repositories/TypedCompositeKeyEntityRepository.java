package com.github.silent.samurai.speedy.repositories;

import com.github.silent.samurai.speedy.entity.TypedCompositeKeyEntity;
import com.github.silent.samurai.speedy.entity.TypedCompositeKeyEntityId;
import org.springframework.data.repository.CrudRepository;

/// Repository for {@link TypedCompositeKeyEntity}, used by tests to seed/tear down rows in the
/// shared H2 context (the entity is addressed only by its {@code @IdClass} composite key).
public interface TypedCompositeKeyEntityRepository
        extends CrudRepository<TypedCompositeKeyEntity, TypedCompositeKeyEntityId> {
}
