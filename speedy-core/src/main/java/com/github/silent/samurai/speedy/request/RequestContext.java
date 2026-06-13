package com.github.silent.samurai.speedy.request;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.conversion.codec.ConversionContext;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpMethod;

import java.util.Map;

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
    /// The conversion context carrying all registries (JavaTypeRegistry, JsonRegistry, etc.)
    /// for the request pipeline. Set at context creation and immutable thereafter.
    ///
    /// @see ConversionContext
    final ConversionContext conversionContext;

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

    public RequestContext(ISpeedyConfiguration configuration, SpeedyDialect dialect, MetaModel metaModel, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, EventProcessor eventProcessor, ValidationProcessor validationProcessor, ConversionContext conversionContext) {
        this.configuration = configuration;
        this.dialect = dialect;
        this.metaModel = metaModel;
        this.httpServletRequest = httpServletRequest;
        this.httpServletResponse = httpServletResponse;
        this.eventProcessor = eventProcessor;
        this.validationProcessor = validationProcessor;
        this.conversionContext = conversionContext;
    }

    /// Shorthand for {@code getRequest().getUriContext().getParsedQuery().getFrom()}
    public EntityMetadata getEntityMetadata() {
        return request.getUriContext().getParsedQuery().getFrom();
    }
}
