package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.annotations.SpeedySensitive;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
// Entity-level sensitivity: all fields are sensitive by default unless
// individually overridden with @SpeedySensitive(false)
@SpeedySensitive
@Table(name = "sensitive_class")
@Entity
public class SensitiveClassEntity extends AbstractBaseEntity {

    // Inherits sensitivity from @SpeedySensitive on the class
    @Column(name = "field_a")
    private String fieldA;

    // Explicitly exempt from sensitivity — allowed in $ field references
    @SpeedySensitive(false)
    @Column(name = "field_b")
    private String fieldB;
}
