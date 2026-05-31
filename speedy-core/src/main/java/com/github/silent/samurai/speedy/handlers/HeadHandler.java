package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.request.RequestContext;

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
public class HeadHandler implements Handler {

    final Handler next;

    public HeadHandler(Handler handler) {
        this.next = handler;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        next.process(context);
    }
}
