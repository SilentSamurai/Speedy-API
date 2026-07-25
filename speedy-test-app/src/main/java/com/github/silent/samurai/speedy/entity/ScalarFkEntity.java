package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.annotations.SpeedyAssociation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

// Exercises @SpeedyAssociation, the manual-association path for scalar fields JPA itself
// doesn't recognize as a relationship (no @ManyToOne/@JoinColumn). Each field below targets a
// primary key of a different type/generation strategy, since the FK's actual read/write
// conversion is driven entirely by the *target* key field's type (JooqUtil.conversionField),
// not by this entity's own declared Java type:
//   - ref / refByName -> PkUuidTest.id       (java.util.UUID, GenerationType.UUID)
//   - catRef           -> Category.id         (String, GenerationType.UUID)
//   - numRef            -> AutoGenIdentityEntity.id (Long, GenerationType.IDENTITY)
@Getter
@Setter
@Table(name = "SCALAR_FK_ENTITY")
@Entity
public class ScalarFkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    protected UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "ref_id", nullable = true)
    @SpeedyAssociation(PkUuidTest.class)
    private UUID ref;

    // Same target, resolved by Speedy entity name instead of Java class.
    @Column(name = "ref_by_name_id", nullable = true)
    @SpeedyAssociation(entity = "PkUuidTest")
    private UUID refByName;

    // Targets a String-typed, GenerationType.UUID primary key.
    @Column(name = "cat_ref_id", nullable = true)
    @SpeedyAssociation(Category.class)
    private String catRef;

    // Targets a numeric, DB-assigned (IDENTITY) primary key.
    @Column(name = "num_ref_id", nullable = true)
    @SpeedyAssociation(AutoGenIdentityEntity.class)
    private Long numRef;

}
