package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.annotations.SpeedyBulk;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/// Test fixture for issue #28: an entity that explicitly opts out of bulk for every write
/// operation via {@code @SpeedyBulk({})}. Single-object and single-element-array requests
/// still work; any multi-element array request is rejected with 400.
@Getter
@Setter
@Table(name = "bulk_disabled_entity")
@Entity
@SpeedyBulk({})
public class BulkDisabledEntity extends AbstractBaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

}
