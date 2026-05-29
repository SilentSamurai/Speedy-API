package com.github.silent.samurai.speedy.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "sensitive_fk")
@Entity
public class SensitiveFkEntity extends AbstractBaseEntity {

    @Column(name = "name")
    private String name;

    // FK to SensitiveTestEntity — used to test that FK traversal
    // ($sensitiveTestEntity.secretField) also respects sensitivity
    @ManyToOne(optional = false)
    @JoinColumn(name = "sensitive_entity_id", nullable = false)
    private SensitiveTestEntity sensitiveTestEntity;
}
