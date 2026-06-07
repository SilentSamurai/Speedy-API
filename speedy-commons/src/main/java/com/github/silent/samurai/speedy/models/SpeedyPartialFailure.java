package com.github.silent.samurai.speedy.models;

import lombok.Builder;
import lombok.Getter;

/// Represents a single failure within a batch create or delete operation.
///
/// Captures the index, error details, and the input primary key that caused
/// the failure.
@Getter
@Builder
public class SpeedyPartialFailure {

    /// Zero-based index of the entity in the original input array.
    private final int index;

    /// HTTP status code for this individual failure.
    private final int status;

    /// Human-readable error message.
    private final String message;

    /// ISO-8601 timestamp of when the failure occurred.
    private final String timestamp;

    /// The primary key of the input entity that caused the failure, if available.
    private final SpeedyEntityKey inputPk;

    /// The underlying exception that caused the failure.
    private final Throwable cause;
}
