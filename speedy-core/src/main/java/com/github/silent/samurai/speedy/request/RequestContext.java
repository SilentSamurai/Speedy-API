package com.github.silent.samurai.speedy.request;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;

/// Per-request state bag that extends {@link SpeedyContext}.
///
/// All request-scoped dependencies and handler results are stored in the
/// typed bag, keyed by their concrete class. The single convenience method
/// {@link #getEntityMetadata()} provides quick access to the resolved entity.
///
/// @see SpeedyContext
public class RequestContext extends SpeedyContext {

    public EntityMetadata getEntityMetadata() {
        return get(SpeedyUriContext.class).getParsedQuery().getFrom();
    }
}
