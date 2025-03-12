package com.github.silent.samurai.speedy.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpMethod;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    EntityMetadata entityMetadata;
    QueryProcessor queryProcessor;
    SpeedyQuery speedyQuery;
    IResponseSerializerV2 responseSerializer;

    String requestUri;
    HttpMethod httpMethod;
    JsonNode body;

    public RequestContext(ISpeedyConfiguration configuration, SpeedyDialect dialect, MetaModel metaModel, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, EventProcessor eventProcessor, ValidationProcessor validationProcessor) {
        this.configuration = configuration;
        this.dialect = dialect;
        this.metaModel = metaModel;
        this.httpServletRequest = httpServletRequest;
        this.httpServletResponse = httpServletResponse;
        this.eventProcessor = eventProcessor;
        this.validationProcessor = validationProcessor;
    }
}
