package com.github.silent.samurai.speedy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/// Minimal owner-tagged entity dedicated to exercising row-level ABAC "only my own rows" policies,
/// where {@link #owner} is compared against the calling principal's id.
@Getter
@Setter
@Table(name = "documents")
@Entity
public class Document extends AbstractBaseEntity {

    @Column(name = "title", nullable = false, length = 250)
    private String title;

    @Column(name = "owner", nullable = false, length = 250)
    private String owner;

}
