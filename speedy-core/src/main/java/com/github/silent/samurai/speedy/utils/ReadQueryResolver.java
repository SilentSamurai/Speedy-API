package com.github.silent.samurai.speedy.utils;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.handlers.GetHandler;
import com.github.silent.samurai.speedy.handlers.QueryHandler;
import com.github.silent.samurai.speedy.handlers.ReadQueryPolicyHandler;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;

/// GET's query comes from the URI parser; {@code $query}'s comes from the parsed JSON body — same
/// tree shape either way. Shared by {@link ReadQueryPolicyHandler}, which runs before
/// {@link GetHandler}/{@link QueryHandler} and needs the query regardless of which parser produced it.
public final class ReadQueryResolver {

    private ReadQueryResolver() {
    }

    public static SpeedyQuery resolve(SpeedyContext context) {
        return context.find(SpeedyBody.class)
                .filter(SpeedyQuery.class::isInstance)
                .map(SpeedyQuery.class::cast)
                .orElseGet(() -> context.get(SpeedyUriContext.class).getParsedQuery());
    }
}
