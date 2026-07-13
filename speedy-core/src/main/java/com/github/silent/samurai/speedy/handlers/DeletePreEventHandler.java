package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.models.SpeedyDeleteBody;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;

/// Fires PRE_DELETE events before the delete transaction is started.
public class DeletePreEventHandler implements Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        SpeedyDeleteBody body = (SpeedyDeleteBody) context.get(SpeedyBody.class);
        EntityMetadata entityMetadata = context.get(SpeedyUriContext.class).getParsedQuery().getFrom();
        EventProcessor eventProcessor = context.get(EventProcessor.class);

        for (var key : body.getKeys()) {
            eventProcessor.triggerEvent(SpeedyEventType.PRE_DELETE, entityMetadata, key);
        }
    }
}
