package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.annotations.SpeedyBulk;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/// Test fixture for issue #28: an entity that opts out of bulk create/delete via
/// {@link SpeedyBulk}. Single-object and single-element-array requests still work;
/// multi-element array requests are rejected with 400.
@Getter
@Setter
@Table(name = "bulk_disabled_entity")
@Entity
@SpeedyBulk(false)
public class BulkDisabledEntity extends AbstractBaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

}
