package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.request.RequestContext;

/// # TailHandler
///
/// Terminates the handler chain. A no-op handler that performs no action.
///
/// ## Purpose
/// - Marks the end of the handler chain
/// - Prevents null pointer exceptions when the last handler calls {@code next.process()}
///
/// ## Chain Position
/// Last handler in the chain. Does not call any next handler.
public class TailHandler implements Handler {

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {

    }
}
