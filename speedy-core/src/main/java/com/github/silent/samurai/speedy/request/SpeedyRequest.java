package com.github.silent.samurai.speedy.request;

import com.github.silent.samurai.speedy.enums.TransactionMode;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyBody;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpMethod;

import java.util.Map;

/// The universal envelope for all Speedy API requests.
///
/// Carries the URI context (from SpeedyUriContext), HTTP metadata
/// (method, URI, headers), transaction mode, and the operation-specific
/// body (SpeedyQuery for read ops, SpeedyCreateBody/UpdateBody/DeleteBody
/// for write ops).
///
/// Built incrementally through the handler chain: UriParserHandler
/// creates it with URI context and HTTP metadata; BodyParserHandler
/// sets the body after parsing.
@Getter
public class SpeedyRequest {

    /// Operation-specific payload, set by BodyParserHandler after parsing.
    @Setter
    private SpeedyBody body;

    /// The URI context produced by SpeedyUriContext.parse().
    private final SpeedyUriContext uriContext;

    /// Transaction mode for this request.
    private final TransactionMode transactionMode;

    /// HTTP method of the request.
    private final HttpMethod httpMethod;

    /// The normalized request URI.
    private final String requestUri;

    /// HTTP request headers as immutable name-value pairs.
    private final Map<String, String> headers;

    public SpeedyRequest(SpeedyUriContext uriContext, TransactionMode transactionMode,
                         HttpMethod httpMethod, String requestUri, Map<String, String> headers) {
        this.uriContext = uriContext;
        this.transactionMode = transactionMode;
        this.httpMethod = httpMethod;
        this.requestUri = requestUri;
        this.headers = headers;
    }

    /// Convenience accessor for the target entity from the URI context.
    public EntityMetadata getEntity() {
        return uriContext.getParsedQuery().getFrom();
    }

    /// Convenience accessor for the URI action suffix ($query, $create, etc.).
    public String getActionSuffix() {
        return uriContext.getActionSuffix();
    }
}
