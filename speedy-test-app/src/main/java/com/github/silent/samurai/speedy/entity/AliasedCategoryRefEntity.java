package com.github.silent.samurai.speedy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

// A native @ManyToOne field ("cat") whose Java name deliberately differs from its target
// entity's name ("Category") -- proves the ConditionFactory association-name fallback (see
// AssociationNameFallbackTest) benefits real JPA relationships identically to @SpeedyAssociation,
// since the fallback operates purely on FieldMetadata/EntityMetadata and has no notion of how
// the association was declared.
@Getter
@Setter
@Table(name = "ALIASED_CATEGORY_REF_ENTITY")
@Entity
public class AliasedCategoryRefEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    protected UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cat_id", nullable = false)
    private Category cat;

}
