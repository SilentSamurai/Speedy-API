package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.annotations.SpeedyBulk;
import com.github.silent.samurai.speedy.enums.BulkOperation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/// Test fixture for selective bulk: bulk is enabled for CREATE only via
/// {@code @SpeedyBulk(BulkOperation.CREATE)}. Multi-element create bodies succeed, while
/// multi-element update/replace/delete bodies are rejected with 400. Single-element bodies
/// work for every operation.
@Getter
@Setter
@Table(name = "bulk_create_only_entity")
@Entity
@SpeedyBulk(BulkOperation.CREATE)
public class BulkCreateOnlyEntity extends AbstractBaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

}
