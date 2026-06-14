package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;

/// # Handler
///
/// Chain of Responsibility contract for the request processing pipeline.
/// Each handler holds a reference to the next handler in the chain and
/// calls {@code next.process(context)} to pass the mutable {@link SpeedyContext}
/// downstream after completing its own work.
///
/// ## Chain Assembly
/// Pre-built sub-chains are wired in {@code SpeedyEngineImpl} for each
/// lifecycle phase: request parsing, body parsing, CRUD operations, and
/// response writing.
///
/// @see com.github.silent.samurai.speedy.handlers package summary
public interface Handler {
    void process(SpeedyContext context) throws SpeedyHttpException;
}
