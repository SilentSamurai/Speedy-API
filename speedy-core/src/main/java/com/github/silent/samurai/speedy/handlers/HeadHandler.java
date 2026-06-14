package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.context.SpeedyContext;

/// # HeadHandler
///
/// Chain entry point. A passthrough decorator that immediately delegates to
/// the next handler without performing any processing itself.
///
/// ## Purpose
/// - Provides a consistent entry point for the chain
/// - Can serve as an instrumentation point for logging, metrics, or security checks
///
/// ## Chain Position
/// First handler in the chain. Always passes control to {@link RequestParserHandler}.
public class HeadHandler implements com.github.silent.samurai.speedy.interfaces.Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
    }
}
