package com.github.silent.samurai.speedy.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.github.silent.samurai.speedy.annotations.SpeedyETag;
import com.github.silent.samurai.speedy.enums.EtagStrategy;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/// A composite-key entity modelling the most common *mixed* real-world composite key: an
/// identifying foreign key plus an effective-dating timestamp. The key is
/// {@code productId} (VARCHAR **foreign key** → {@link Product}) + {@code effectiveAt}
/// (TIMESTAMP / {@link LocalDateTime}).
/// <p>
/// It fills two gaps left by the other composite-key entities: {@code Order} keys on two String
/// FKs (no temporal column), and {@code TypedCompositeKeyEntity} keys on plain scalars (no FK). This
/// one exercises FK-resolution and temporal-column coercion *within the same key* — proving Speedy
/// round-trips a {@code TIMESTAMP} key column through GET-by-key (as a URL query param),
/// {@code $query} filters and {@code $update}/{@code $delete} bodies.
@Getter
@Setter
@Table(name = "price_history")
@Entity
@IdClass(PriceHistoryId.class)
public class PriceHistory implements Serializable {

    @Id
    @Column(name = "product_id", nullable = false, length = 250)
    private String productId;

    @Id
    @Column(name = "effective_at", nullable = false)
    private LocalDateTime effectiveAt;

    @Column(name = "price")
    private Double price;

    @Column(name = "note", length = 500)
    private String note;

    /// Speedy-managed conditional-request token (TIMESTAMP strategy), stamped fresh on every
    /// create/update/replace. Covers the mixed-composite-key + `@SpeedyETag` combination.
    @SpeedyETag(strategy = EtagStrategy.TIMESTAMP)
    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    /// Read-only association overlaying the {@code product_id} key column, so the key column is a
    /// genuine foreign key (mirrors {@link Order#getProduct()}). The scalar {@code productId} owns
    /// the column; this is {@code insertable=false, updatable=false}.
    @JsonBackReference
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false, insertable = false, updatable = false)
    private Product product;

}
