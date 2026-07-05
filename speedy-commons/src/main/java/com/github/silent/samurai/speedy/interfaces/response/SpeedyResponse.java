package com.github.silent.samurai.speedy.interfaces.response;

import com.github.silent.samurai.speedy.enums.SpeedyResponseType;

import java.util.Map;

/// The response produced by a Speedy API operation.
///
/// Implementations carry the data to be serialized (entities, counts, or batch
/// results) along with HTTP metadata such as status code and headers.
public interface SpeedyResponse {

    /// The type of response, indicating what kind of payload it contains.
    SpeedyResponseType getType();

    /// The HTTP status code to be sent to the client.
    int getStatus();

    /// HTTP headers to include in the response as an immutable map of name-value pairs.
    Map<String, String> getHeaders();
}
