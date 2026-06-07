package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.enums.TransactionMode;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.request.SpeedyRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;

import java.util.Map;

/// Parses the request URI via SpeedyUriContext and builds the SpeedyRequest.
///
/// Uses SpeedyUriContext to parse the URI into entity metadata, action suffix,
/// and a URI-based SpeedyQuery. Resolves the transaction mode and constructs
/// the SpeedyRequest envelope that flows through the remainder of the chain.
///
/// @see SpeedyUriContext
/// @see SpeedyRequest
public class UriParserHandler implements Handler {

    final Handler next;

    public UriParserHandler(Handler next) {
        this.next = next;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        MetaModel metaModel = context.getMetaModel();
        String requestURI = context.getRequestUri();

        SpeedyUriContext parser = SpeedyUriContext.builder()
                .metaModel(metaModel)
                .requestURI(requestURI)
                .maxPageSize(context.getConfiguration().getMaxPageSize())
                .defaultPageSize(context.getConfiguration().getDefaultPageSize())
                .maxQueryStringLength(context.getConfiguration().getMaxQueryStringLength())
                .maxFilterCount(context.getConfiguration().getMaxFilterCount())
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

        next.process(context);
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
