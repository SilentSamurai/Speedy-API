package com.github.silent.samurai.speedy.enums;

/// How Speedy refreshes a {@code @SpeedyETag}-annotated field on every create/update/replace.
public enum EtagStrategy {

    /// A fresh opaque token (random UUID) written on create and every update. Requires a
    /// text-valued column; needs no prior value and no extra fetch.
    RANDOM,

    /// The field is set to the current timestamp on create and every update. Requires a
    /// temporal-valued column (date/time/date-time/zoned-date-time).
    TIMESTAMP

}
