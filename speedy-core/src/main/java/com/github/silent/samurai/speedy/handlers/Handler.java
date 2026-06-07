package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.request.RequestContext;

/// # Handler
///
/// Chain of Responsibility contract for the request processing pipeline.
/// Each handler holds a reference to the next handler in the chain and
/// calls {@code next.process(context)} to pass the mutable {@link RequestContext}
/// downstream after completing its own work.
///
/// ## Chain Assembly
/// The chain is wired in {@code SpeedyFactory.createHandlerChain()} with a
/// specific order: HeadHandler → RequestParserHandler → UriParserHandler →
/// SwitchHandler → [CRUDhandlers] → SpeedyResponseWriterHandler → TailHandler.
///
/// @see com.github.silent.samurai.speedy.handlers package summary
public interface Handler {
    void process(RequestContext context) throws SpeedyHttpException;
}
