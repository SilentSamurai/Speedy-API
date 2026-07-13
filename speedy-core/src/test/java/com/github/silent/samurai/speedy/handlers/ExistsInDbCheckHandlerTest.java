package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.interfaces.backend.QueryProcessor;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.models.SpeedyDeleteBody;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.models.SpeedyHeaders;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class ExistsInDbCheckHandlerTest {

    @Test
    void deleteTargetsAreFetchedOnceAndReusedDownstream() throws Exception {
        QueryProcessor queryProcessor = mock(QueryProcessor.class);
        SpeedyEntityKey firstKey = mock(SpeedyEntityKey.class);
        SpeedyEntityKey secondKey = mock(SpeedyEntityKey.class);
        SpeedyEntity firstRow = mock(SpeedyEntity.class);
        SpeedyEntity secondRow = mock(SpeedyEntity.class);
        when(queryProcessor.fetchByKey(firstKey)).thenReturn(Optional.of(firstRow));
        when(queryProcessor.fetchByKey(secondKey)).thenReturn(Optional.of(secondRow));

        SpeedyDeleteBody body = SpeedyDeleteBody.builder()
                .keys(List.of(firstKey, secondKey))
                .build();
        SpeedyContext context = new SpeedyContext()
                .put(QueryProcessor.class, queryProcessor)
                .put(SpeedyBody.class, body)
                .put(new SpeedyHeaders(Map.of()));

        new ExistsInDbCheckHandler().process(context);

        DbCheckEntities existingEntities = context.get(DbCheckEntities.class);
        assertSame(firstRow, existingEntities.get(firstKey));
        assertSame(secondRow, existingEntities.get(secondKey));
        verify(queryProcessor).fetchByKey(firstKey);
        verify(queryProcessor).fetchByKey(secondKey);
        verify(queryProcessor, never()).findExistingKeys(anyList());
        verifyNoMoreInteractions(queryProcessor);
    }
}
