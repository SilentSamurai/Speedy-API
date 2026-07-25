package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.annotations.SpeedyAssociation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

// Reproduces a downstream-reported NotFoundException: a @SpeedyAssociation field named with the
// conventional DB/Kotlin "Id" suffix (e.g. "productId") is exposed under that literal Java member
// name, NOT the suffix-stripped form ("product") that a caller might assume mirrors @ManyToOne
// naming (see docs/speedy-jpa.md's `target` example, which carries no suffix at all). Querying
// "product.id" against this entity fails with "Field 'product' not found in entity SuffixedFkEntity";
// the correct path is "productId.id".
@Getter
@Setter
@Table(name = "SUFFIXED_FK_ENTITY")
@Entity
public class SuffixedFkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    protected UUID id;

    @Column(name = "product_id", nullable = true)
    @SpeedyAssociation(entity = "PkUuidTest")
    private UUID productId;

}
