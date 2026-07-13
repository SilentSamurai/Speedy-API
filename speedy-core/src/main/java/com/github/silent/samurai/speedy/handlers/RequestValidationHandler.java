package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.models.SpeedyCreateBody;
import com.github.silent.samurai.speedy.models.SpeedyDeleteBody;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.models.SpeedyUpdateBody;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/// Performs the validation appropriate for one request operation.
///
/// Instances are wired into the operation chains after parsing, authorization, and precondition
/// checks, and before the terminal operation handler. Terminal handlers do not invoke this handler.
public class RequestValidationHandler implements Handler {

    private static final Set<SpeedyRequestType> SUPPORTED_REQUESTS = EnumSet.of(
            SpeedyRequestType.GET_LIST,
            SpeedyRequestType.QUERY,
            SpeedyRequestType.CREATE,
            SpeedyRequestType.UPDATE,
            SpeedyRequestType.REPLACE,
            SpeedyRequestType.DELETE);

    private final SpeedyRequestType requestType;

    public RequestValidationHandler(SpeedyRequestType requestType) {
        this.requestType = Objects.requireNonNull(requestType, "requestType must not be null");
        if (!SUPPORTED_REQUESTS.contains(requestType)) {
            throw new IllegalArgumentException("Validation is not supported for request type: " + requestType);
        }
    }

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        ValidationProcessor validationProcessor = validationProcessor(context);
        switch (requestType) {
            case GET_LIST -> validateQuery(context,
                    context.get(SpeedyUriContext.class).getParsedQuery());
            case QUERY -> validateQuery(context, (SpeedyQuery) context.get(SpeedyBody.class));
            case CREATE -> {
                SpeedyCreateBody body = (SpeedyCreateBody) context.get(SpeedyBody.class);
                EntityMetadata entityMetadata = entityMetadata(context);
                for (var entity : body.getEntities()) {
                    validationProcessor.validateCreateRequestEntity(entityMetadata, entity);
                }
            }
            case UPDATE, REPLACE -> {
                SpeedyUpdateBody body = (SpeedyUpdateBody) context.get(SpeedyBody.class);
                EntityMetadata entityMetadata = entityMetadata(context);
                for (SpeedyUpdateBody.Item item : body.getItems()) {
                    if (requestType == SpeedyRequestType.UPDATE) {
                        validationProcessor.validateUpdateRequestEntity(entityMetadata, item.getEntity());
                    } else {
                        validationProcessor.validateReplaceRequestEntity(entityMetadata, item.getEntity());
                    }
                }
            }
            case DELETE -> {
                SpeedyDeleteBody body = (SpeedyDeleteBody) context.get(SpeedyBody.class);
                EntityMetadata entityMetadata = entityMetadata(context);
                for (SpeedyEntityKey key : body.getKeys()) {
                    validationProcessor.validateDeleteRequestEntity(entityMetadata, key);
                }
            }
            default -> throw new IllegalStateException("Unsupported validation request type: " + requestType);
        }
    }

    private void validateQuery(SpeedyContext context, SpeedyQuery query) throws SpeedyHttpException {
        validationProcessor(context).validateQueryRequest(query);
    }

    private ValidationProcessor validationProcessor(SpeedyContext context) {
        return context.get(ValidationProcessor.class);
    }

    private EntityMetadata entityMetadata(SpeedyContext context) {
        return context.get(SpeedyUriContext.class).getParsedQuery().getFrom();
    }
}
