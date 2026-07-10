package com.github.silent.samurai.speedy.client.format;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Wire-format codec for the Speedy API client — the client-side mirror of the
 * server's {@code ISpeedyIoProvider} SPI.
 *
 * <p>Builders always assemble requests as Jackson trees; the format only decides
 * how those trees are rendered on the wire and how response bodies are parsed
 * back. {@link #read} must return the standard JSON envelope shape
 * ({@code {"payload":[...],"pageIndex":N,...}}, {@code {"count":N}},
 * {@code {"message":...,"timestamp":...}}) so response handling stays
 * format-agnostic.
 *
 * <p>Configured once per client via {@code Speedy.builder().format(...)};
 * defaults to {@link JsonFormat}.
 */
public interface SpeedyFormat {

    /**
     * The media type sent as {@code Content-Type} and {@code Accept},
     * e.g. {@code "application/json;charset=UTF-8"}. The server negotiates by
     * substring match on the bare mime type.
     */
    String contentType();

    /**
     * Serializes a request tree to the wire representation.
     *
     * @throws RuntimeException if the tree cannot be serialized
     */
    String write(JsonNode tree);

    /**
     * Parses a response body into the standard JSON envelope tree.
     *
     * @throws com.github.silent.samurai.speedy.client.exception.SpeedyDeserializationException
     *         if the body is not valid for this format
     */
    JsonNode read(String body);
}
