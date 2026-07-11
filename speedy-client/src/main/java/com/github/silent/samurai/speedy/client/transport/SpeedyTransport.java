package com.github.silent.samurai.speedy.client.transport;

import java.io.IOException;

/**
 * Pluggable HTTP transport backend for the Speedy API client.
 * Framework-agnostic contract using only String, Map, and int types.
 *
 * <p>The transport MUST:
 * <ul>
 *   <li>Forward all headers from {@link SpeedyRequest#headers()} to the HTTP request</li>
 *   <li>Default {@code Content-Type: application/json;charset=UTF-8} when the request body is
 *       non-null and the request carries no {@code Content-Type} header — never override one
 *       that is present (the client sets it from its configured wire format)</li>
 *   <li>Default {@code Accept: application/json;charset=UTF-8} when the request carries no
 *       {@code Accept} header</li>
 *   <li>NOT throw on 4xx/5xx responses — return the response with status code intact</li>
 *   <li>Only throw {@link IOException} for network-level failures</li>
 * </ul>
 */
@FunctionalInterface
public interface SpeedyTransport {

    /**
     * Sends an HTTP request and returns the raw response.
     *
     * @param request the request to send
     * @return the raw HTTP response (status code, headers, body)
     * @throws IOException for network-level failures (connection refused, timeout, DNS, SSL)
     */
    SpeedyRawResponse send(SpeedyRequest request) throws IOException;
}
