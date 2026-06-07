package com.github.silent.samurai.speedy.request;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.IRequestBodyParser;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializerV2;
import com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.interfaces.SpeedyResponse;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpMethod;

import java.util.Map;

/// Mutable state object that flows through the entire handler chain.
///
/// Carries immutable infrastructure (configuration, dialect, meta model, servlet
/// objects, event/validation processors) set at construction, and mutable fields
/// progressively populated by handlers: HTTP metadata, SpeedyRequest, raw body
/// bytes, operation type, body parser, query processor, response serializer,
/// and the final SpeedyResponse.
@Setter
@Getter
public class RequestContext {

    final ISpeedyConfiguration configuration;
    final SpeedyDialect dialect;
    final MetaModel metaModel;
    final HttpServletRequest httpServletRequest;
    final HttpServletResponse httpServletResponse;
    final EventProcessor eventProcessor;
    final ValidationProcessor validationProcessor;

    HttpMethod httpMethod;
    String requestUri;
    Map<String, String> headers;
    SpeedyRequestType requestType;
    SpeedyRequest request;
    byte[] rawBody;
    IRequestBodyParser requestBodyParser;
    QueryProcessor queryProcessor;
    IResponseSerializerV2 responseSerializer;
    SpeedyResponse speedyResponse;

    public RequestContext(ISpeedyConfiguration configuration, SpeedyDialect dialect, MetaModel metaModel, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, EventProcessor eventProcessor, ValidationProcessor validationProcessor) {
        this.configuration = configuration;
        this.dialect = dialect;
        this.metaModel = metaModel;
        this.httpServletRequest = httpServletRequest;
        this.httpServletResponse = httpServletResponse;
        this.eventProcessor = eventProcessor;
        this.validationProcessor = validationProcessor;
    }

    /// Shorthand for {@code getRequest().getUriContext().getParsedQuery().getFrom()}
    public EntityMetadata getEntityMetadata() {
        return request.getUriContext().getParsedQuery().getFrom();
    }
}
