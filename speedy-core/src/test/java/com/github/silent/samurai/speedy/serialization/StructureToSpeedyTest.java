package com.github.silent.samurai.speedy.serialization;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.request.StructureReader;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Structural branches of the format-agnostic {@link StructureToSpeedy} builder driven by an
/// {@link InMemoryStructureReader} (no format module): scalar collections, deserializable /
/// unknown field skipping, and key completeness. The JSON strings are only a convenient way to
/// build the input token tree.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StructureToSpeedyTest {

    private final StructureToSpeedy builder = new StructureToSpeedy();

    private SpeedyEntity parse(EntityMetadata entityMetadata, String json) throws SpeedyHttpException {
        StructureReader r = new InMemoryStructureReader(readTree(json));
        r.begin();
        return builder.fromEntity(entityMetadata, r);
    }

    private Object readTree(String json) throws SpeedyHttpException {
        try {
            return CommonUtil.json().readValue(json, Object.class);
        } catch (IOException e) {
            throw new BadRequestException("invalid test json", e);
        }
    }

    @Test
    void scalarCollectionField() throws SpeedyHttpException {
        FieldMetadata field = mock(FieldMetadata.class);
        when(field.getOutputPropertyName()).thenReturn("tags");
        when(field.isDeserializable()).thenReturn(true);
        when(field.isAssociation()).thenReturn(false);
        when(field.isCollection()).thenReturn(true);
        when(field.getValueType()).thenReturn(ValueType.INT);
        EntityMetadata entityMetadata = mock(EntityMetadata.class);
        when(entityMetadata.has("tags")).thenReturn(true);
        when(entityMetadata.field("tags")).thenReturn(field);

        SpeedyEntity entity = parse(entityMetadata, "{\"tags\":[1,2,3]}");

        SpeedyValue value = entity.get(field);
        assertTrue(value.isCollection());
        assertEquals(3, value.asCollection().size());
    }

    @Test
    void nonDeserializableFieldSkipped() throws SpeedyHttpException {
        FieldMetadata field = mock(FieldMetadata.class);
        when(field.getOutputPropertyName()).thenReturn("secret");
        when(field.isDeserializable()).thenReturn(false);
        EntityMetadata entityMetadata = mock(EntityMetadata.class);
        when(entityMetadata.has("secret")).thenReturn(true);
        when(entityMetadata.field("secret")).thenReturn(field);

        SpeedyEntity entity = parse(entityMetadata, "{\"secret\":\"x\"}");

        assertFalse(entity.has(field));
    }

    @Test
    void unknownFieldSkipped() throws SpeedyHttpException {
        EntityMetadata entityMetadata = mock(EntityMetadata.class);
        when(entityMetadata.has("unknown")).thenReturn(false);

        SpeedyEntity entity = parse(entityMetadata, "{\"unknown\":\"x\"}");

        assertEquals(ValueType.OBJECT, entity.getValueType());
    }

    @Test
    void incompleteKeyDetected() {
        KeyFieldMetadata keyField = mock(KeyFieldMetadata.class);
        when(keyField.getOutputPropertyName()).thenReturn("id");
        EntityMetadata entityMetadata = mock(EntityMetadata.class);
        when(entityMetadata.getKeyFields()).thenReturn(Set.of(keyField));

        SpeedyEntity entity = new SpeedyEntity(entityMetadata);

        assertFalse(builder.isKeyComplete(entityMetadata, entity));
    }
}
