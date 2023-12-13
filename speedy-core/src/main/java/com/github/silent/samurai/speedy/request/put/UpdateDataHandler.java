package com.github.silent.samurai.speedy.request.put;

import com.github.silent.samurai.speedy.enums.SpeedyEventType;
import com.github.silent.samurai.speedy.events.EventProcessor;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyVirtualEntityHandler;
import com.github.silent.samurai.speedy.interfaces.query.QueryProcessor;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class UpdateDataHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDataHandler.class);

    private final PutRequestContext context;

    public UpdateDataHandler(PutRequestContext context) {
        this.context = context;
    }

    public Optional<Object> process() throws Exception {
        EntityMetadata entityMetadata = context.getEntityMetadata();
        EventProcessor eventProcessor = context.getEventProcessor();
        Object savedEntity = null;
        SpeedyEntity entity = context.getEntity();
        SpeedyEntityKey entityKey = context.getEntityKey();
        if (entity != null) {
            context.getValidationProcessor().validateUpdateRequestEntity(entityMetadata, entity);

            eventProcessor.triggerEvent(SpeedyEventType.PRE_UPDATE,
                    entityMetadata, entity);
            if (context.getVEntityProcessor().isVirtualEntity(entityMetadata)) {
                SpeedyVirtualEntityHandler handler = context.getVEntityProcessor().getHandler(entityMetadata);
                savedEntity = handler.update(entityKey, entity);
            } else {
                QueryProcessor queryProcessor = context.getQueryProcessor();
//                savedEntity = saveEntity(entityInstance, entityMetadata);
                queryProcessor.update(entityKey, entity);
            }
            eventProcessor.triggerEvent(SpeedyEventType.POST_UPDATE,
                    entityMetadata, entity);

        }

        return Optional.ofNullable(savedEntity);
    }
}
