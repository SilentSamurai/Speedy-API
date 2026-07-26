package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.annotations.SpeedyAssociation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

// The realistic @SpeedyAssociation shape: a legacy scalar FK whose Java field is named after the
// column it maps ("catId"), not after the reference it becomes. Left as-is the association would be
// exposed as "catId" and followed as "catId.id" -- the Id suffix lying about a property that is now
// an object. name() renames it to what a @ManyToOne field would have been called, so it reads
// "cat.id", identical to AliasedCategoryRefEntity's native relationship.
//
// Kept off ScalarFkEntity deliberately: that entity's Category-targeting field (catRef) must stay
// the only one for AssociationNameFallbackTest's unambiguous-entity-name case.
@Getter
@Setter
@Table(name = "RENAMED_SCALAR_FK_ENTITY")
@Entity
public class RenamedScalarFkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    protected UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "cat_id", nullable = true)
    @SpeedyAssociation(value = Category.class, name = "cat")
    private String catId;

}
