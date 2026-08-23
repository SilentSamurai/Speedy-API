package com.github.silent.samurai.speedy.data;


import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

/// An entity keyed on a single temporal column. {@link Product} and {@link UniqueProduct} key on
/// strings, so nothing exercised a key whose stored form a dialect may have to canonicalise before
/// comparing it — the case that only exists on a backend with no temporal storage class.
@Data
public class TimestampKeyedReading {

    @Id
    private LocalDateTime recordedAt;

    String label;

}
