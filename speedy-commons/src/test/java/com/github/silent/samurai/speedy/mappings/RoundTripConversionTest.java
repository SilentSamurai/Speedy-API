package com.github.silent.samurai.speedy.mappings;

import com.github.silent.samurai.speedy.data.StaticEntityMetadata;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.models.*;
import com.github.silent.samurai.speedy.utils.SpeedyValueFactory;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RoundTripConversionTest {

    @Getter
    @Setter
    public static class TestEntity {

        @Id
        public UUID id;

        // Basic types
        private String name;
        private Integer age;
        private boolean active;
        private Double salary;

        // Numeric types
        private Long longValue;
        private Float floatValue;
        private BigInteger bigIntegerValue;
        private BigDecimal bigDecimalValue;

        // Date/Time types
        private LocalDate date;
        private LocalDateTime dateTime;
        private ZonedDateTime zonedDateTime;

        // Default constructor
        public TestEntity() {
        }
    }

    @Test
    void convertSpeedyEntityToJavaObjectAndBack_shouldMaintainDataIntegrity() throws SpeedyHttpException {
        // Arrange
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(TestEntity.class);

        // Test values
        String testName = "John Doe";
        Integer testAge = 30;
        boolean testActive = true;
        double testSalary = 75000.50;
        UUID testId = UUID.randomUUID();
        long testLongValue = 1234567890L;
        Float testFloatValue = 42.5f;
        BigInteger testBigIntegerValue = new BigInteger("9876543210");
        BigDecimal testBigDecimalValue = new BigDecimal("12345.6789");
        LocalDate testDate = LocalDate.of(2023, 6, 15);
        LocalDateTime testDateTime = LocalDateTime.of(2023, 6, 15, 14, 30, 45);
        ZonedDateTime testZonedDateTime = ZonedDateTime.now();

        // Create SpeedyEntity with test data
        SpeedyEntity originalEntity = new SpeedyEntity(entityMetadata);
        originalEntity.put("name", new SpeedyText(testName));
        originalEntity.put("age", new SpeedyInt(testAge.longValue()));
        originalEntity.put("active", new SpeedyBoolean(testActive));
        originalEntity.put("salary", new SpeedyDouble(testSalary));
        originalEntity.put("id", new SpeedyText(testId.toString()));
        originalEntity.put("longValue", new SpeedyInt(testLongValue));
        originalEntity.put("floatValue", new SpeedyDouble(testFloatValue.doubleValue()));
        originalEntity.put("bigIntegerValue", new SpeedyInt(testBigIntegerValue.longValue()));
        originalEntity.put("bigDecimalValue", new SpeedyDouble(testBigDecimalValue.doubleValue()));
        originalEntity.put("date", new SpeedyDate(testDate));
        originalEntity.put("dateTime", new SpeedyDateTime(testDateTime));
        originalEntity.put("zonedDateTime", new SpeedyZonedDateTime(testZonedDateTime));

        // Act - Convert SpeedyEntity to a Java object
        TestEntity javaObject = SpeedySerializer.toJavaEntity(originalEntity, TestEntity.class);

        // Then convert a Java object back to SpeedyEntity
        SpeedyEntity convertedBackEntity = SpeedyDeserializer.updateEntity(javaObject, new SpeedyEntity(entityMetadata));

        // Assert
        assertNotNull(javaObject);
        assertNotNull(convertedBackEntity);

        // Verify that the data is consistent after round-trip conversion
        assertEquals(testName, convertedBackEntity.get("name").asText());
        assertEquals(testAge.longValue(), convertedBackEntity.get("age").asLong());
        assertEquals(testActive, convertedBackEntity.get("active").asBoolean());
        assertEquals(testSalary, convertedBackEntity.get("salary").asDouble(), 0.001);
        assertEquals(testId.toString(), convertedBackEntity.get("id").asText());
        assertEquals(testLongValue, convertedBackEntity.get("longValue").asLong());
        assertEquals(testFloatValue.doubleValue(), convertedBackEntity.get("floatValue").asDouble(), 0.001);
        assertEquals(testBigIntegerValue.longValue(), convertedBackEntity.get("bigIntegerValue").asLong());
        assertEquals(testBigDecimalValue.doubleValue(), convertedBackEntity.get("bigDecimalValue").asDouble(), 0.001);
        assertEquals(testDate, convertedBackEntity.get("date").asDate());
        assertEquals(testDateTime, convertedBackEntity.get("dateTime").asDateTime());
        assertEquals(testZonedDateTime.toInstant(), convertedBackEntity.get("zonedDateTime").asZonedDateTime().toInstant());
    }

    @Test
    void convertJavaObjectToSpeedyEntityAndBack_shouldMaintainDataIntegrity() throws Exception {
        // Arrange
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(TestEntity.class);
        SpeedyEntity entity = new SpeedyEntity(entityMetadata);

        // Test values
        String testName = "John Doe";
        Integer testAge = 30;
        boolean testActive = true;
        double testSalary = 75000.50;
        UUID testId = UUID.randomUUID();
        long testLongValue = 1234567890L;
        Float testFloatValue = 42.5f;
        BigInteger testBigIntegerValue = new BigInteger("9876543210");
        BigDecimal testBigDecimalValue = new BigDecimal("12345.6789");
        LocalDate testDate = LocalDate.of(2023, 6, 15);
        LocalDateTime testDateTime = LocalDateTime.of(2023, 6, 15, 14, 30, 45);
        ZonedDateTime testZonedDateTime = ZonedDateTime.now();

        // Create TestEntity with all test data
        TestEntity testEntity = new TestEntity();
        testEntity.setId(testId);
        testEntity.setName(testName);
        testEntity.setAge(testAge);
        testEntity.setActive(testActive);
        testEntity.setSalary(testSalary);
        testEntity.setLongValue(testLongValue);
        testEntity.setFloatValue(testFloatValue);
        testEntity.setBigIntegerValue(testBigIntegerValue);
        testEntity.setBigDecimalValue(testBigDecimalValue);
        testEntity.setDate(testDate);
        testEntity.setDateTime(testDateTime);
        testEntity.setZonedDateTime(testZonedDateTime);

        // Act - Convert a Java object to SpeedyEntity
        SpeedyEntity speedyEntity = SpeedyDeserializer.updateEntity(testEntity, entity);

        // Then convert SpeedyEntity back to a Java object
        TestEntity convertedBackObject = SpeedySerializer.toJavaEntity(speedyEntity, TestEntity.class);

        // Assert
        assertNotNull(speedyEntity);
        assertNotNull(convertedBackObject);

        // Verify that the data is consistent after round-trip conversion
        assertEquals(testName, convertedBackObject.getName());
        assertEquals(testAge, convertedBackObject.getAge());
        assertEquals(testActive, convertedBackObject.isActive());
        assertEquals(testSalary, convertedBackObject.getSalary(), 0.001);
        assertEquals(testId, convertedBackObject.getId());
        assertEquals(testLongValue, convertedBackObject.getLongValue());
        assertEquals(testFloatValue, convertedBackObject.getFloatValue());
        assertEquals(testBigIntegerValue, convertedBackObject.getBigIntegerValue());
        assertEquals(testBigDecimalValue, convertedBackObject.getBigDecimalValue());
        assertEquals(testDate, convertedBackObject.getDate());
        assertEquals(testDateTime, convertedBackObject.getDateTime());
        assertEquals(testZonedDateTime.toInstant(), convertedBackObject.getZonedDateTime().toInstant());
    }

    @Test
    void convertJavaObjectToSpeedyEntityAndBack_withNullValues_shouldMaintainDataIntegrity() throws Exception {
        // Arrange
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(TestEntity.class);
        SpeedyEntity entity = new SpeedyEntity(entityMetadata);

        // Create TestEntity with null values
        TestEntity testEntity = new TestEntity();
        testEntity.setId(null);
        testEntity.setName(null);
        testEntity.setAge(null);
        testEntity.setActive(false); // Can't be null for primitive boolean
        testEntity.setSalary(null);
        testEntity.setLongValue(null);
        testEntity.setFloatValue(null);
        testEntity.setBigIntegerValue(null);
        testEntity.setBigDecimalValue(null);
        testEntity.setDate(null);
        testEntity.setDateTime(null);
        testEntity.setZonedDateTime(null);

        // Act - Convert Java object to SpeedyEntity
        SpeedyEntity speedyEntity = SpeedyDeserializer.updateEntity(testEntity, entity);

        // Then convert SpeedyEntity back to Java object
        TestEntity convertedBackObject = SpeedySerializer.toJavaEntity(speedyEntity, TestEntity.class);

        // Assert
        assertNotNull(speedyEntity);
        assertNotNull(convertedBackObject);

        // Verify that the data is consistent after round-trip conversion
        assertNull(convertedBackObject.getName());
        assertNull(convertedBackObject.getAge());
        assertFalse(convertedBackObject.isActive()); // Primitive boolean can't be null
        assertNull(convertedBackObject.getSalary());
        assertNull(convertedBackObject.getId());
        assertNull(convertedBackObject.getLongValue());
        assertNull(convertedBackObject.getFloatValue());
        assertNull(convertedBackObject.getBigIntegerValue());
        assertNull(convertedBackObject.getBigDecimalValue());
        assertNull(convertedBackObject.getDate());
        assertNull(convertedBackObject.getDateTime());
        assertNull(convertedBackObject.getZonedDateTime());
    }

    @Test
    void setValuesInClass_convertToSpeedyEntity_convertBackToClass_andAssertValues() throws Exception {
        // Arrange - Set values in class
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(TestEntity.class);
        SpeedyEntity entity = new SpeedyEntity(entityMetadata);
        
        UUID id = UUID.randomUUID();
        String name = "Test User";
        Integer age = 25;
        boolean active = true;
        Double salary = 50000.0;
        Long longValue = 1000000L;
        Float floatValue = 3.14f;
        BigInteger bigIntegerValue = new BigInteger("123456789");
        BigDecimal bigDecimalValue = new BigDecimal("98765.4321");
        LocalDate date = LocalDate.now();
        LocalDateTime dateTime = LocalDateTime.now();
        ZonedDateTime zonedDateTime = ZonedDateTime.now();

        TestEntity originalEntity = new TestEntity();
        originalEntity.setId(id);
        originalEntity.setName(name);
        originalEntity.setAge(age);
        originalEntity.setActive(active);
        originalEntity.setSalary(salary);
        originalEntity.setLongValue(longValue);
        originalEntity.setFloatValue(floatValue);
        originalEntity.setBigIntegerValue(bigIntegerValue);
        originalEntity.setBigDecimalValue(bigDecimalValue);
        originalEntity.setDate(date);
        originalEntity.setDateTime(dateTime);
        originalEntity.setZonedDateTime(zonedDateTime);

        // Act - Convert to Speedy entity
        SpeedyEntity speedyEntity = SpeedyDeserializer.updateEntity(originalEntity, entity);
        
        // Convert back to class
        TestEntity convertedEntity = SpeedySerializer.toJavaEntity(speedyEntity, TestEntity.class);

        // Assert - Check all values
        assertNotNull(convertedEntity);
        assertEquals(id, convertedEntity.getId());
        assertEquals(name, convertedEntity.getName());
        assertEquals(age, convertedEntity.getAge());
        assertEquals(active, convertedEntity.isActive());
        assertEquals(salary, convertedEntity.getSalary());
        assertEquals(longValue, convertedEntity.getLongValue());
        assertEquals(floatValue, convertedEntity.getFloatValue());
        assertEquals(bigIntegerValue, convertedEntity.getBigIntegerValue());
        assertEquals(bigDecimalValue, convertedEntity.getBigDecimalValue());
        assertEquals(date, convertedEntity.getDate());
        assertEquals(dateTime, convertedEntity.getDateTime());
        assertEquals(zonedDateTime.toInstant(), convertedEntity.getZonedDateTime().toInstant());
    }

    @Test
    void nullValueInClassField_shouldNotOverrideExistingSpeedyEntityValue_duringRoundTrip() throws Exception {
        // Arrange - Create a SpeedyEntity with initial values
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(TestEntity.class);
        SpeedyEntity initialEntity = new SpeedyEntity(entityMetadata);
        
        // Set initial values in the SpeedyEntity
        UUID initialId = UUID.randomUUID();
        String initialName = "Initial Name";
        Integer initialAge = 30;
        boolean initialActive = true;
        Double initialSalary = 60000.0;
        
        initialEntity.put("id", SpeedyValueFactory.fromText(initialId.toString()));
        initialEntity.put("name", SpeedyValueFactory.fromText(initialName));
        initialEntity.put("age", SpeedyValueFactory.fromInt(initialAge.longValue()));
        initialEntity.put("active", SpeedyValueFactory.fromBool(initialActive));
        initialEntity.put("salary", SpeedyValueFactory.fromDouble(initialSalary));
        
        // Convert SpeedyEntity to Java object
        TestEntity javaObject = SpeedySerializer.toJavaEntity(initialEntity, TestEntity.class);
        
        // Set some fields to null in the Java object
        javaObject.setName(null);
        javaObject.setAge(null);
        javaObject.setSalary(null);
        
        // Act - Convert the Java object (with null values) back to SpeedyEntity
        SpeedyEntity finalEntity = SpeedyDeserializer.updateEntity(javaObject, initialEntity);
        
        // Assert - Verify that the original values are preserved for fields that were null in the Java object
        assertNotNull(finalEntity);
        assertEquals(initialId.toString(), finalEntity.get("id").asText());
        assertTrue(finalEntity.get("name").isText());
        assertEquals(initialName, finalEntity.get("name").asText()); // Should preserve initial value
        assertEquals(initialAge.intValue(), finalEntity.get("age").asInt().intValue()); // Should preserve initial value
        assertEquals(initialActive, finalEntity.get("active").asBoolean()); // Boolean is primitive, so can't be null
        assertEquals(initialSalary, finalEntity.get("salary").asDouble(), 0.001); // Should preserve initial value

    }
}
