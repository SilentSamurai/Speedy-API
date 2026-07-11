package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.models.SpeedyCreateBody;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyUpdateBody;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.utils.EtagManager;

import java.util.List;
import java.util.Optional;

/// Stamps a fresh Speedy-managed ETag value (per the entity's declared {@code @SpeedyETag}
/// strategy) into every entity in the parsed body, before the create/update/replace handler
/// persists it — this is what makes the token change on every write. Entities with no version
/// field are untouched: a single {@link EntityMetadata#getVersionField()} check, no other work.
///
/// @see EtagManager
public class EtagStampHandler implements Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();
        Optional<FieldMetadata> versionField = entityMetadata.getVersionField();
        if (versionField.isEmpty()) {
            return;
        }
        FieldMetadata etagField = versionField.get();

        for (SpeedyEntity entity : entitiesIn(context.get(SpeedyBody.class))) {
            entity.put(etagField, EtagManager.freshValue(etagField));
        }
    }

    private List<SpeedyEntity> entitiesIn(SpeedyBody body) {
        if (body instanceof SpeedyCreateBody createBody) {
            return createBody.getEntities();
        }
        if (body instanceof SpeedyUpdateBody updateBody) {
            return updateBody.getItems().stream().map(SpeedyUpdateBody.Item::getEntity).toList();
        }
        return List.of();
    }
}
