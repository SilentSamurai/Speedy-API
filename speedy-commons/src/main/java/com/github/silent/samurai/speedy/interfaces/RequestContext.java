package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Optional;

@Setter
@Getter
public class RequestContext {

    final MetaModel metaModel;
    final HttpServletRequest httpServletRequest;
    final HttpServletResponse httpServletResponse;


    EntityMetadata entityMetadata;
    QueryProcessor queryProcessor;
    Optional<List<SpeedyEntity>> requestedData = Optional.empty();
    SpeedyQuery speedyQuery;

    int serializationType;
    int pageNo;
    List<String> expands;
    String requestUri;
    HttpMethod httpMethod;

    public RequestContext(MetaModel metaModel, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        this.metaModel = metaModel;
        this.httpServletRequest = httpServletRequest;
        this.httpServletResponse = httpServletResponse;
    }
}
