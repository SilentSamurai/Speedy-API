package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.enums.TransactionMode;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.conversion.registry.JavaTypeRegistry;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.request.SpeedyRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;

import java.util.Map;

public class UriParserHandler implements Handler {

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        MetaModel metaModel = context.getMetaModel();
        String requestURI = context.getRequestUri();
        /// Extract the Java-type registry from the conversion context so that
        /// {@link SpeedyUriContext} can parse URL query-parameter values.
        JavaTypeRegistry jtr = context.getConversionContext().get(JavaTypeRegistry.class);

        SpeedyUriContext parser = SpeedyUriContext.builder()
                .metaModel(metaModel)
                .requestURI(requestURI)
                .maxPageSize(context.getConfiguration().getMaxPageSize())
                .defaultPageSize(context.getConfiguration().getDefaultPageSize())
                .maxQueryStringLength(context.getConfiguration().getMaxQueryStringLength())
                .maxFilterCount(context.getConfiguration().getMaxFilterCount())
                .javaTypeRegistry(jtr)
                .build();
        SpeedyQuery uriSpeedyQuery = parser.parse();

        EntityMetadata resourceMetadata = uriSpeedyQuery.getFrom();

        TransactionMode effectiveMode = resolveTransactionMode(
                context.getHttpServletRequest(),
                resourceMetadata
        );

        HttpMethod httpMethod = context.getHttpMethod();
        Map<String, String> headers = context.getHeaders();

        SpeedyRequest request = new SpeedyRequest(parser, effectiveMode, httpMethod, requestURI, headers);
        context.setRequest(request);
    }

    private TransactionMode resolveTransactionMode(
            HttpServletRequest request,
            EntityMetadata entityMetadata) throws SpeedyHttpException {
        String override = request.getParameter("$transaction");
        TransactionMode entityDefault = entityMetadata.getTransactionMode();

        if (override == null || override.isBlank()) {
            return entityDefault;
        }

        TransactionMode requested = parseTransactionMode(override);
        return validateOverride(requested, entityDefault, entityMetadata.getName());
    }

    private TransactionMode parseTransactionMode(String value) throws SpeedyHttpException {
        try {
            return TransactionMode.valueOf(value.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Invalid $transaction value: '" + value +
                            "'. Allowed values: per-entity, batch"
            );
        }
    }

    private TransactionMode validateOverride(
            TransactionMode requested,
            TransactionMode entityDefault,
            String entityName) throws SpeedyHttpException {
        if (entityDefault == TransactionMode.PER_ENTITY
                && requested == TransactionMode.BATCH) {
            return requested;
        }
        if (requested == entityDefault) {
            return requested;
        }
        throw new BadRequestException(
                "Entity '" + entityName + "' is configured with transaction mode 'batch'. " +
                        "Cannot downgrade to 'per-entity' per request. " +
                        "Remove the $transaction parameter or use 'batch'."
        );
    }
}
