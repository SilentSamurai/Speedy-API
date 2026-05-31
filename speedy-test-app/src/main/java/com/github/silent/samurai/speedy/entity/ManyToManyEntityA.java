package com.github.silent.samurai.speedy.entity;

import jakarta.persistence.*;
import java.util.Set;

/// Test entity with a `@ManyToMany` association using `@JoinTable`.
///
/// Used by `ManyToManyAssociationIT` to verify that `JpaMetaModelProcessorV2`
/// gracefully skips `@ManyToMany` associations with a warning log instead of
/// crashing, allowing the entity itself (and its PK) to still be registered
/// in the metamodel.
@Entity
@Table(name = "many_to_many_entity_a")
public class ManyToManyEntityA {

    @Id
    @Column(name = "id")
    private String id;

    @ManyToMany
    @JoinTable(name = "entity_a_b",
               joinColumns = @JoinColumn(name = "a_id"),
               inverseJoinColumns = @JoinColumn(name = "b_id"))
    private Set<ManyToManyEntityB> entitiesB;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Set<ManyToManyEntityB> getEntitiesB() { return entitiesB; }
    public void setEntitiesB(Set<ManyToManyEntityB> entitiesB) { this.entitiesB = entitiesB; }
}
