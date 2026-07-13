package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.models.SpeedyUpdateBody;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;

/// Fires PRE_UPDATE events before update/replace validation and the write transaction.
///
/// Shared by the PATCH and PUT chains. PRE_UPDATE handlers populate event-managed fields that
/// validation then checks, so this must run before {@link RequestValidationHandler}. A throwing
/// event aborts the whole request before any transaction is opened.
public class UpdatePreEventHandler implements Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        SpeedyUpdateBody body = (SpeedyUpdateBody) context.get(SpeedyBody.class);
        if (body.getItems().isEmpty()) {
            return;
        }
        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();
        EventProcessor eventProcessor = context.get(EventProcessor.class);

        for (SpeedyUpdateBody.Item item : body.getItems()) {
            eventProcessor.triggerEvent(SpeedyEventType.PRE_UPDATE, entityMetadata, item.getEntity());
        }
    }
}
