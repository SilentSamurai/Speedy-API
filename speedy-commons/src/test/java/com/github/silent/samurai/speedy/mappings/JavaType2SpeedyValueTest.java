package com.github.silent.samurai.speedy.mappings;

import com.github.silent.samurai.speedy.data.StaticEntityMetadata;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JavaType2SpeedyValueTest {

    @Test
    void convertAndSetField_withValidValue_shouldUpdateEntity() throws Exception {
        // Arrange
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(TestEntity.class);
        SpeedyEntity entity = new SpeedyEntity(entityMetadata);
        TestEntity testEntity = new TestEntity("testName", 25);
        Field field = TestEntity.class.getDeclaredField("name");

        // Act
        JavaType2SpeedyValue.convertAndSetField(entity, field, testEntity);

        // Assert
        assertEquals("testName", entity.get("name").asText());
    }

    @Test
    void convertFromCompositeClass_withValidObject_shouldReturnSpeedyEntity() throws Exception {
        // Arrange
        TestEntity testEntity = new TestEntity("testName", 25);
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(TestEntity.class);

        SpeedyEntity entity = new SpeedyEntity(entityMetadata);

        // Act
        SpeedyEntity result = JavaType2SpeedyValue.convertFromCompositeClass(testEntity, entity);

        // Assert
        assertNotNull(result);
        assertEquals("testName", result.get("name").asText());
        assertEquals(25L, result.get("age").asInt().longValue());
    }

    @Test
    void convertFromCompositeClass_withNullValue_shouldReturnNull() throws Exception {
        // Act
        SpeedyEntity result = JavaType2SpeedyValue.convertFromCompositeClass(null, new SpeedyEntity(StaticEntityMetadata.createEntityMetadata(TestEntity.class)));

        // Assert
        assertNull(result);
    }

    @Test
    void convertFromCompositeClass_withAllTypes_shouldMapFieldsCorrectly() throws Exception {
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
        Instant testInstant = Instant.now();
        Timestamp testTimestamp = Timestamp.valueOf("2023-06-15 14:30:45");
        ZonedDateTime testZonedDateTime = ZonedDateTime.now();
        Date testUtilDate = new Date();
        java.sql.Date testSqlDate = java.sql.Date.valueOf("2023-06-15");
        LocalTime testTime = LocalTime.of(14, 30, 45);

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
        testEntity.setInstant(testInstant);
        testEntity.setTimestamp(testTimestamp);
        testEntity.setZonedDateTime(testZonedDateTime);
        testEntity.setUtilDate(testUtilDate);
        testEntity.setSqlDate(testSqlDate);
        testEntity.setTime(testTime);

        // Act
        SpeedyEntity result = JavaType2SpeedyValue.convertFromCompositeClass(testEntity, entity);

        // Assert
        assertNotNull(result);
        assertEquals(testName, result.get("name").asText());
        assertEquals(testAge.longValue(), result.get("age").asInt().longValue());
        assertEquals(testActive, result.get("active").asBoolean());
        assertEquals(testSalary, result.get("salary").asDouble(), 0.001);
        assertEquals(testId.toString(), result.get("id").asText());
        assertEquals(testLongValue, result.get("longValue").asInt().longValue());
        assertEquals(testFloatValue.doubleValue(), result.get("floatValue").asDouble(), 0.001);
        assertEquals(testBigIntegerValue.longValue(), result.get("bigIntegerValue").asLong());
        assertEquals(testBigDecimalValue.doubleValue(), result.get("bigDecimalValue").asDouble(), 0.001);
        assertEquals(testDate, result.get("date").asDate());
        assertEquals(testDateTime, result.get("dateTime").asDateTime());
        assertNotNull(result.get("instant"));
        assertNotNull(result.get("timestamp"));
        assertNotNull(result.get("zonedDateTime"));
        assertNotNull(result.get("utilDate"));
        assertEquals(testSqlDate.toString(), result.get("sqlDate").asDate().toString());
        assertEquals(testTime, result.get("time").asTime());
    }

    @Getter
    @Setter
    static class TestEntity {
        @Id
        public UUID id;

        // Basic types
        String name;
        Integer age;
        boolean active;
        Double salary;

        // Numeric types
        Long longValue;
        Float floatValue;
        BigInteger bigIntegerValue;
        BigDecimal bigDecimalValue;

        // Date/Time types
        LocalDate date;
        LocalDateTime dateTime;
        Instant instant;
        Timestamp timestamp;
        ZonedDateTime zonedDateTime;
        Date utilDate;
        java.sql.Date sqlDate;

        // Time type
        LocalTime time;

        // Default constructor
        public TestEntity() {
        }

        // Constructor for convenience
        public TestEntity(String name, Integer age) {
            this.name = name;
            this.age = age;
        }
    }


}
