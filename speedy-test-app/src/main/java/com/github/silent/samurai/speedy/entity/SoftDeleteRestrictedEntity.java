package com.github.silent.samurai.speedy.entity;

import com.github.silent.samurai.speedy.annotations.SpeedySoftDelete;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/// Test fixture for soft-delete security gating (issue #116): soft delete is enabled, but the
/// entity does NOT allow viewing deleted rows ({@code allowViewDeleted = false}) or permanent
/// deletion ({@code allowHardDelete = false}). Used to assert that {@code $deleted=include|only}
/// is rejected with 403 and {@code $purge} is rejected with 403 for a non-opted-in entity.
@Getter
@Setter
@Table(name = "soft_delete_restricted_entity")
@Entity
@SpeedySoftDelete(field = "deletedAt")
public class SoftDeleteRestrictedEntity extends AbstractBaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

}
