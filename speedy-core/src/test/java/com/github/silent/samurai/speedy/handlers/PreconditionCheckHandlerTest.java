package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.models.*;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;

class PreconditionCheckHandlerTest {

    @Test
    void ifMatchUsesThePreflightedRowWithoutRefetching() throws Exception {
        EntityMetadata entityMetadata = mock(EntityMetadata.class);
        FieldMetadata versionField = mock(FieldMetadata.class);
        when(entityMetadata.getVersionField()).thenReturn(Optional.of(versionField));

        SpeedyEntityKey key = mock(SpeedyEntityKey.class);
        SpeedyEntity row = new SpeedyEntity(entityMetadata);
        row.put(versionField, new SpeedyText("version-1"));
        SpeedyDeleteBody body = SpeedyDeleteBody.builder().keys(List.of(key)).build();

        SpeedyQueryImpl query = mock(SpeedyQueryImpl.class);
        when(query.getFrom()).thenReturn(entityMetadata);
        SpeedyUriContext uriContext = mock(SpeedyUriContext.class);
        when(uriContext.getParsedQuery()).thenReturn(query);
        QueryProcessor queryProcessor = mock(QueryProcessor.class);

        SpeedyContext context = new SpeedyContext()
                .put(SpeedyBody.class, body)
                .put(new SpeedyHeaders(Map.of("If-Match", "W/\"version-1\"")))
                .put(new DbCheckEntities(Map.of(key, row)))
                .put(SpeedyUriContext.class, uriContext)
                .put(QueryProcessor.class, queryProcessor);

        new PreconditionCheckHandler().process(context);

        verifyNoInteractions(queryProcessor);
    }
}
