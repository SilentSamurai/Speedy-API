package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.models.SpeedyUpdateBody;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;

/// Handles PUT /{Entity}/$update requests: a *full replace* where the pre-parsed
/// {@link SpeedyUpdateBody} is treated as the complete representation of the resource —
/// required fields are enforced (400 if missing) and omitted nullable non-key fields are
/// reset to null. Idempotent by construction.
///
/// @see AbstractUpdateHandler
/// @see UpdateHandler
/// @see UpdateBodyParserHandler
public class ReplaceHandler extends AbstractUpdateHandler {

    @Override
    protected void validate(ValidationProcessor validationProcessor, EntityMetadata entityMetadata,
                            SpeedyEntity entity) throws SpeedyHttpException {
        validationProcessor.validateReplaceRequestEntity(entityMetadata, entity);
    }

    @Override
    protected SpeedyEntity persist(QueryProcessor queryProcessor, SpeedyEntityKey pk,
                                   SpeedyEntity entity) throws SpeedyHttpException {
        return queryProcessor.replace(pk, entity);
    }

    @Override
    protected String operationLabel() {
        return "Replace";
    }

    @Override
    protected PermissionType permission() {
        return PermissionType.REPLACE;
    }
}
