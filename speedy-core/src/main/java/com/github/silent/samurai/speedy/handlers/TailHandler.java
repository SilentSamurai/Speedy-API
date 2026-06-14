package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.context.SpeedyContext;

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
public class TailHandler implements com.github.silent.samurai.speedy.interfaces.Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {

    }
}
