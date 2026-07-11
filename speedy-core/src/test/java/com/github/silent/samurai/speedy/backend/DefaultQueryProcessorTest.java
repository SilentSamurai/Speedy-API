package com.github.silent.samurai.speedy.backend;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpRuntimeException;
import com.github.silent.samurai.speedy.interfaces.backend.SpeedyBackend;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.models.SpeedyEntityKey;
import com.github.silent.samurai.speedy.models.SpeedyText;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefaultQueryProcessorTest {

    @Test
    void createFailsWhenBackendOmitsDatabaseGeneratedKey() throws Exception {
        SpeedyBackend backend = mock(SpeedyBackend.class);
        DefaultQueryProcessor processor = new DefaultQueryProcessor(backend);
        EntityMetadata metadata = mock(EntityMetadata.class);
        KeyFieldMetadata id = keyField("id", false, false);
        FieldMetadata name = field("name");
        when(metadata.getAllFields()).thenReturn(Set.of(id, name));
        when(metadata.getKeyFields()).thenReturn(Set.of(id));

        SpeedyEntity entity = new SpeedyEntity(metadata);
        entity.put(name, new SpeedyText("Ada"));

        InternalServerError exception = assertThrows(InternalServerError.class,
                () -> processor.create(List.of(entity)));

        assertEquals(500, exception.getStatus());
        assertTrue(exception.getMessage().contains("did not return database-generated key 'id'"));
        verify(backend).insert(List.of(entity));
        verify(backend, never()).selectByKeys(anyList());
    }

    @Test
    void deleteNeverDelegatesEmptyOrKeylessKeys() throws Exception {
        SpeedyBackend backend = mock(SpeedyBackend.class);
        DefaultQueryProcessor processor = new DefaultQueryProcessor(backend);

        assertTrue(processor.delete(List.of()).isEmpty());
        verifyNoInteractions(backend);

        EntityMetadata keylessMetadata = mock(EntityMetadata.class);
        when(keylessMetadata.getKeyFields()).thenReturn(Set.of());
        when(keylessMetadata.getName()).thenReturn("KeylessAuditRow");

        assertTrue(processor.delete(List.of(new SpeedyEntityKey(keylessMetadata))).isEmpty());
        verifyNoInteractions(backend);
    }

    @Test
    void translatesNativeBackendExceptionsAndPreservesHttpRuntimeStatus() throws Exception {
        SpeedyBackend backend = mock(SpeedyBackend.class);
        DefaultQueryProcessor processor = new DefaultQueryProcessor(backend);
        SpeedyQuery query = mock(SpeedyQuery.class);
        RuntimeException nativeFailure = new RuntimeException("unique constraint");
        BadRequestException mapped = new BadRequestException("invalid value", nativeFailure);

        when(backend.count(query)).thenThrow(nativeFailure);
        when(backend.classify(nativeFailure)).thenReturn(Optional.of(mapped));

        BadRequestException translated = assertThrows(BadRequestException.class,
                () -> processor.executeCount(query));
        assertSame(mapped, translated);

        SpeedyHttpRuntimeException runtimeFailure = new SpeedyHttpRuntimeException(429, "slow down");
        doThrow(runtimeFailure).when(backend).count(query);

        SpeedyHttpException runtimeTranslated = assertThrows(SpeedyHttpException.class,
                () -> processor.executeCount(query));
        assertEquals(429, runtimeTranslated.getStatus());
        assertEquals("slow down", runtimeTranslated.getMessage());
        assertSame(runtimeFailure, runtimeTranslated.getCause());
    }

    @Test
    void executeManyWithCountMapsRowsAndKeepsTheBackendCount() throws Exception {
        SpeedyBackend backend = mock(SpeedyBackend.class);
        DefaultQueryProcessor processor = new DefaultQueryProcessor(backend);
        EntityMetadata metadata = mock(EntityMetadata.class);
        FieldMetadata name = field("name");
        when(metadata.getAllFields()).thenReturn(Set.of(name));

        SpeedyEntity row = new SpeedyEntity(metadata);
        row.put(name, new SpeedyText("Ada"));
        SpeedyQuery query = mock(SpeedyQuery.class);
        when(query.getFrom()).thenReturn(metadata);
        when(query.getExpand()).thenReturn(Set.of());
        when(backend.count(query)).thenReturn(BigInteger.ONE);
        when(backend.select(query)).thenReturn(List.of(row));

        var result = processor.executeManyWithCount(query);

        assertEquals(BigInteger.ONE, result.totalCount());
        assertEquals(List.of(row), result.entities());
        verify(backend).count(query);
        verify(backend).select(query);
    }

    private static KeyFieldMetadata keyField(String name, boolean insertable, boolean shouldGenerate) {
        KeyFieldMetadata field = mock(KeyFieldMetadata.class);
        when(field.getOutputPropertyName()).thenReturn(name);
        when(field.isInsertable()).thenReturn(insertable);
        when(field.shouldGenerateKey()).thenReturn(shouldGenerate);
        when(field.isDatabaseGenerated()).thenReturn(!insertable && !shouldGenerate);
        when(field.isAssociation()).thenReturn(false);
        return field;
    }

    private static FieldMetadata field(String name) {
        FieldMetadata field = mock(FieldMetadata.class);
        when(field.getOutputPropertyName()).thenReturn(name);
        when(field.isAssociation()).thenReturn(false);
        return field;
    }
}
