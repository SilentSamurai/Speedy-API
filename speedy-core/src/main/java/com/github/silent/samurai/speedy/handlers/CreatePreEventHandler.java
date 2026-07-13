package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.models.SpeedyCreateBody;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;

/// Fires PRE_INSERT events before create validation and the write transaction.
///
/// PRE_INSERT handlers populate event-managed fields (defaults, timestamps) that validation then
/// checks, so this must run before {@link RequestValidationHandler}. A throwing event aborts the
/// whole request before any transaction is opened.
public class CreatePreEventHandler implements Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        SpeedyCreateBody body = (SpeedyCreateBody) context.get(SpeedyBody.class);
        if (body.getEntities().isEmpty()) {
            return;
        }
        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();
        EventProcessor eventProcessor = context.get(EventProcessor.class);

        for (SpeedyEntity entity : body.getEntities()) {
            eventProcessor.triggerEvent(SpeedyEventType.PRE_INSERT, entityMetadata, entity);
        }
    }
}
