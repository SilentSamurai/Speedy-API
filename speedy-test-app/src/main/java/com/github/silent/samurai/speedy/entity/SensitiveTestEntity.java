package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.annotations.SpeedySensitive;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "sensitive_test")
@Entity
public class SensitiveTestEntity extends AbstractBaseEntity {

    // Blocked from $ field references: ?otherField=$secretField will be rejected
    @SpeedySensitive
    @Column(name = "secret_field")
    private String secretField;

    @Column(name = "public_field")
    private String publicField;

    // Also blocked from $ field references
    @SpeedySensitive
    @Column(name = "amount")
    private Double amount;
}
