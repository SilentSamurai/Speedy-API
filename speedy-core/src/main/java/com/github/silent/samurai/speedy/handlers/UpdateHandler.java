package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.models.SpeedyUpdateBody;
import com.github.silent.samurai.speedy.validation.ValidationProcessor;

/// Handles PATCH /{Entity}/$update requests: a *partial* update where only the fields
/// supplied in the pre-parsed {@link SpeedyUpdateBody} are written and omitted fields are
/// left untouched. Required-field enforcement is relaxed (supplied fields only).
///
/// @see AbstractUpdateHandler
/// @see ReplaceHandler
/// @see UpdateBodyParserHandler
public class UpdateHandler extends AbstractUpdateHandler {

    @Override
    protected void validate(ValidationProcessor validationProcessor, EntityMetadata entityMetadata,
                            SpeedyEntity entity) throws SpeedyHttpException {
        validationProcessor.validateUpdateRequestEntity(entityMetadata, entity);
    }

    @Override
    protected SpeedyEntity persist(QueryProcessor queryProcessor, SpeedyEntityKey pk,
                                   SpeedyEntity entity) throws SpeedyHttpException {
        return queryProcessor.update(pk, entity);
    }

    @Override
    protected String operationLabel() {
        return "Update";
    }

    @Override
    protected PermissionType permission() {
        return PermissionType.UPDATE;
    }
}
