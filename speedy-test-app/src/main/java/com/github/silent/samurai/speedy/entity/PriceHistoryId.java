package com.github.silent.samurai.speedy.entity;

import jakarta.persistence.Column;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/// {@code @IdClass} for {@link PriceHistory}. Mirrors its two key columns:
/// {@code productId} (VARCHAR FK) + {@code effectiveAt} (TIMESTAMP). Implements equals/hashCode
/// (via Lombok) as required for an {@code @IdClass}.
@Getter
@Setter
@EqualsAndHashCode
public class PriceHistoryId implements Serializable {

    @Column(name = "product_id", nullable = false, length = 250)
    private String productId;

    @Column(name = "effective_at", nullable = false)
    private LocalDateTime effectiveAt;

}
