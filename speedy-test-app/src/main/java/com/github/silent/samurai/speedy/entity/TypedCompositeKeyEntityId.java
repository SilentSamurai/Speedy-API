package com.github.silent.samurai.speedy.entity;

import jakarta.persistence.Column;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

/// {@code @IdClass} for {@link TypedCompositeKeyEntity}. Mirrors its three key columns
/// ({@code numId}, {@code effectiveDate}, {@code code}). Implements equals/hashCode (via Lombok)
/// as required for an {@code @IdClass}.
@Getter
@Setter
@EqualsAndHashCode
public class TypedCompositeKeyEntityId implements Serializable {

    @Column(name = "num_id", nullable = false)
    private Long numId;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

}
