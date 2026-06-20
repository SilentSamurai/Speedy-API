package com.github.silent.samurai.speedy.interfaces.query.backend;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;

import java.util.Optional;

/// Session-level concerns that are inherently backend-specific and cannot live in the
/// format-agnostic core: transaction control and native-exception classification.
public interface BackendSession {

    /// Runs {@code block} inside a backend transaction; reads/writes issued by the core during the
    /// block must see the transactional context.
    void runInTransaction(Runnable block);

    /// Classifies a native backend exception into a {@link SpeedyHttpException} when the backend can
    /// recognise it as a client error (e.g. a SQL integrity/constraint violation &rarr;
    /// {@code BadRequestException}); {@link Optional#empty()} when it is not recognised, in which
    /// case the core falls back to a generic server error.
    Optional<SpeedyHttpException> classify(Exception cause);
}
