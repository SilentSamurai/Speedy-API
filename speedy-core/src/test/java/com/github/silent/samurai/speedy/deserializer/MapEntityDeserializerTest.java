package com.github.silent.samurai.speedy.deserializer;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MapEntityDeserializerTest {

    private EntityMetadata entityMetadata;
    private EntityManager entityManager;
    private Map<String, String> entityMap;
    private MapEntityDeserializer deserializer;

    @BeforeEach
    void setUp() {
        entityMetadata = mock(EntityMetadata.class);
        entityManager = mock(EntityManager.class);
        entityMap = new HashMap<>();
        deserializer = new MapEntityDeserializer(entityMap, entityMetadata, entityManager);
    }

    @Test
    void testDeserializeWithValidFields() throws Exception {
        // Step 1: Setup Field Metadata for different ValueTypes
        FieldMetadata textField = mockFieldMetadata("name", ValueType.TEXT, true);
        FieldMetadata intField = mockFieldMetadata("age", ValueType.INT, true);
        FieldMetadata dateField = mockFieldMetadata("birthDate", ValueType.DATE, true);
        FieldMetadata dateTimeField = mockFieldMetadata("createdAt", ValueType.DATE_TIME, true);
        FieldMetadata timeField = mockFieldMetadata("meetingTime", ValueType.TIME, true);

        when(entityMetadata.getAllFields()).thenReturn(
                Set.of(textField, intField, dateField, dateTimeField, timeField)
        );

        // Step 2: Populate entity map with string representations of field values
        entityMap.put("name", "John Doe");
        entityMap.put("age", "30");
        entityMap.put("birthDate", LocalDate.of(1990, 1, 1).toString());
        entityMap.put("createdAt", LocalDateTime.of(2023, 9, 15, 12, 0).toString());
        entityMap.put("meetingTime", LocalTime.of(14, 30).toString());

        // Step 3: Call deserialize method
        SpeedyEntity entity = deserializer.deserialize();

        // Step 4: Verify that values are properly deserialized and added to the entity
        assertNotNull(entity);
        assertTrue(entity.has(textField));
        assertTrue(entity.has(intField));
        assertTrue(entity.has(dateField));
        assertTrue(entity.has(dateTimeField));
        assertTrue(entity.has(timeField));

        // Verify that values are correct
        assertEquals("John Doe", entity.get(textField).asText());
        assertEquals(30L, entity.get(intField).asLong());
        assertEquals(LocalDate.of(1990, 1, 1), entity.get(dateField).asDate());
        assertEquals(LocalDateTime.of(2023, 9, 15, 12, 0), entity.get(dateTimeField).asDateTime());
        assertEquals(LocalTime.of(14, 30), entity.get(timeField).asTime());
    }

    @Test
    void testDeserializeWithMissingFields() throws Exception {
        // Step 1: Setup Field Metadata
        FieldMetadata textField = mockFieldMetadata("name", ValueType.TEXT, true);
        FieldMetadata intField = mockFieldMetadata("age", ValueType.INT, true);

        when(entityMetadata.getAllFields()).thenReturn(Set.of(textField, intField));

        // Step 2: Populate entity map with only one field
        entityMap.put("name", "John Doe");

        // Step 3: Call deserialize method
        SpeedyEntity entity = deserializer.deserialize();

        // Step 4: Verify deserialization, only one field should be present
        assertNotNull(entity);
        assertTrue(entity.has(textField));
        assertFalse(entity.has(intField));  // Missing field should not be in the entity

        assertEquals("John Doe", entity.get(textField).asText());
    }

    @Test
    void testDeserializeWithNonDeserializableField() throws Exception {
        // Step 1: Setup Field Metadata, one field is non-deserializable
        FieldMetadata textField = mockFieldMetadata("name", ValueType.TEXT, true);
        FieldMetadata nonDeserializableField = mockFieldMetadata("nonDeserializable", ValueType.TEXT, false);

        when(entityMetadata.getAllFields()).thenReturn(Set.of(textField, nonDeserializableField));

        // Step 2: Populate entity map
        entityMap.put("name", "John Doe");
        entityMap.put("nonDeserializable", "ShouldNotDeserialize");

        // Step 3: Call deserialize method
        SpeedyEntity entity = deserializer.deserialize();

        // Step 4: Verify that only the deserializable field is present
        assertNotNull(entity);
        assertTrue(entity.has(textField));
        assertFalse(entity.has(nonDeserializableField));  // Non-deserializable field should not be added

        assertEquals("John Doe", entity.get(textField).asText());
    }

    // Helper method to create mock FieldMetadata
    private FieldMetadata mockFieldMetadata(String fieldName, ValueType valueType, boolean isDeserializable) {
        FieldMetadata fieldMetadata = mock(FieldMetadata.class);
        when(fieldMetadata.getOutputPropertyName()).thenReturn(fieldName);
        when(fieldMetadata.getValueType()).thenReturn(valueType);
        when(fieldMetadata.isDeserializable()).thenReturn(isDeserializable);
        return fieldMetadata;
    }
}
