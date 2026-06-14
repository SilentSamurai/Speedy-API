package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.context.SpeedyContext;

/// # HeadHandler
///
/// No-op entry point at the start of every sub-chain. Used as the first element
/// in each {@code List<Handler>} so all sub-chains have a uniform starting
/// reference point.
///
/// ## Purpose
/// - Provides a consistent first element for every sub-chain
/// - Can serve as an instrumentation point for logging, metrics, or security checks
public class HeadHandler implements com.github.silent.samurai.speedy.interfaces.Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
    }
}
