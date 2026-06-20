package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.context.SpeedyContext;

/// # TailHandler
///
/// No-op terminator at the end of every sub-chain. Used as the last element
/// in each {@code List<Handler>} so all sub-chains have a uniform ending
/// reference point.
public class TailHandler implements com.github.silent.samurai.speedy.interfaces.Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {

    }
}
