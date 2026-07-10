package com.github.silent.samurai.speedy.client;

import com.github.silent.samurai.speedy.client.transport.SpeedyRequest;

/**
 * Request interceptor for modifying outgoing requests before they are sent.
 * Typically used for auth headers, tracing IDs, and multi-tenancy tokens.
 *
 * <p>Interceptors are chained in registration order. Each interceptor receives
 * the current request and returns a (possibly modified) request.
 *
 * <pre>{@code
 * SpeedyInterceptor authInterceptor = req ->
 *     req.withHeader("Authorization", "Bearer " + token);
 * }</pre>
 */
@FunctionalInterface
public interface SpeedyInterceptor {

    /**
     * Intercepts and potentially modifies a request before it is dispatched.
     *
     * @param request the current request state
     * @return the request to send (may be the same or a modified instance)
     */
    SpeedyRequest intercept(SpeedyRequest request);
}
