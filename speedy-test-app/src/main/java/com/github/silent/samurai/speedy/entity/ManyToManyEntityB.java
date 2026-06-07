package com.github.silent.samurai.speedy.entity;

import jakarta.persistence.*;

import java.util.Set;

/// Inverse side of the `@ManyToMany` association with `mappedBy = "entitiesB"`.
///
/// Used alongside `ManyToManyEntityA` by `ManyToManyAssociationIT`.
@Entity
@Table(name = "many_to_many_entity_b")
public class ManyToManyEntityB {

    @Id
    @Column(name = "id")
    private String id;

    @ManyToMany(mappedBy = "entitiesB")
    private Set<ManyToManyEntityA> entitiesA;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<ManyToManyEntityA> getEntitiesA() {
        return entitiesA;
    }

    public void setEntitiesA(Set<ManyToManyEntityA> entitiesA) {
        this.entitiesA = entitiesA;
    }
}
