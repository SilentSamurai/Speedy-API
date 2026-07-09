package com.github.silent.samurai.speedy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

/// A composite-key entity whose key spans three *different* value types — {@code numId} (BIGINT),
/// {@code effectiveDate} (DATE) and {@code code} (VARCHAR). {@link Order}, the only other
/// composite-key entity, keys on two {@code String} columns that are also foreign keys, so it never
/// exercises numeric or temporal key handling. This entity's key columns are plain scalars (no FK),
/// letting tests prove Speedy round-trips non-String key columns through query-param GET-by-key,
/// {@code $query} filters and {@code $update}/{@code $delete} bodies with arbitrary values.
@Getter
@Setter
@Table(name = "typed_composite_key")
@Entity
@IdClass(TypedCompositeKeyEntityId.class)
public class TypedCompositeKeyEntity implements Serializable {

    @Id
    @Column(name = "num_id", nullable = false)
    private Long numId;

    @Id
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Id
    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "description", length = 500)
    private String description;

}
