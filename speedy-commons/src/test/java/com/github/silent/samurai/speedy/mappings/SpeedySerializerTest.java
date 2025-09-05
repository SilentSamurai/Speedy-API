package com.github.silent.samurai.speedy.mappings;

import com.github.silent.samurai.speedy.data.StaticEntityMetadata;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.models.*;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SpeedySerializerTest {

    @Test
    void convertToTargetClassToCompositeClass_withValidEntity_shouldMapFieldsCorrectly() throws SpeedyHttpException {
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
        Instant testInstant = Instant.now();
        Timestamp testTimestamp = Timestamp.valueOf("2023-06-15 14:30:45");
        ZonedDateTime testZonedDateTime = ZonedDateTime.now();
        Date testUtilDate = new Date();
        java.sql.Date testSqlDate = java.sql.Date.valueOf("2023-06-15");

        // Create SpeedyEntity with test data for all fields
        SpeedyEntity entity = new SpeedyEntity(entityMetadata);
        entity.put("name", new SpeedyText(testName));
        entity.put("age", new SpeedyInt(testAge.longValue()));
        entity.put("active", new SpeedyBoolean(testActive));
        entity.put("salary", new SpeedyDouble(testSalary));
        entity.put("id", new SpeedyText(testId.toString()));
        entity.put("longValue", new SpeedyInt(testLongValue));
        entity.put("floatValue", new SpeedyDouble(testFloatValue.doubleValue()));
        entity.put("bigIntegerValue", new SpeedyInt(testBigIntegerValue.longValue()));
        entity.put("bigDecimalValue", new SpeedyDouble(testBigDecimalValue.doubleValue()));
        entity.put("date", new SpeedyDate(testDate));
        entity.put("dateTime", new SpeedyDateTime(testDateTime));
        entity.put("instant", new SpeedyDateTime(testDateTime)); // Using DateTime for Instant
        entity.put("timestamp", new SpeedyDateTime(testDateTime)); // Using DateTime for Timestamp
        entity.put("zonedDateTime", new SpeedyZonedDateTime(testZonedDateTime));
        entity.put("utilDate", new SpeedyDate(testDate)); // Using Date for Util Date
        entity.put("sqlDate", new SpeedyDate(testDate)); // Using Date for SQL Date

        // Act
        TestEntity result = SpeedySerializer.toJavaObject(entity, TestEntity.class);

        // Assert
        assertNotNull(result);
        assertEquals(testName, result.getName());
        assertEquals(testAge, result.getAge());
        assertEquals(testActive, result.isActive());
        assertEquals(testSalary, result.getSalary());
        assertEquals(testId, result.getId());
        assertEquals(testLongValue, result.getLongValue());
        assertEquals(testFloatValue, result.getFloatValue());
        assertEquals(testBigIntegerValue, result.getBigIntegerValue());
        assertEquals(testBigDecimalValue, result.getBigDecimalValue());
        assertEquals(testDate, result.getDate());
        assertEquals(testDateTime, result.getDateTime());
        assertNotNull(result.getInstant());
        assertNotNull(result.getTimestamp());
        assertEquals(testZonedDateTime, result.getZonedDateTime());
        assertNotNull(result.getUtilDate());
        assertEquals(testSqlDate, result.getSqlDate());
    }

    @Test
    void convertToTargetClassToCompositeClass_withAssociation_shouldMapNestedEntity() throws SpeedyHttpException {
        // Arrange
        EntityMetadata parentMd = StaticEntityMetadata.createEntityMetadata(ParentEntity.class);
        SpeedyEntity parent = new SpeedyEntity(parentMd);

        // Build child entity
        EntityMetadata childMd = StaticEntityMetadata.createEntityMetadata(ChildEntity.class);
        SpeedyEntity child = new SpeedyEntity(childMd);
        java.util.UUID childId = java.util.UUID.randomUUID();
        child.put("id", new SpeedyText(childId.toString()));
        child.put("label", new SpeedyText("child-label"));

        // Build parent with nested child
        java.util.UUID parentId = java.util.UUID.randomUUID();
        parent.put("id", new SpeedyText(parentId.toString()));
        parent.put("name", new SpeedyText("parent-name"));
        parent.put("child", child);

        // Act
        ParentEntity result = SpeedySerializer.toJavaObject(parent, ParentEntity.class);

        // Assert
        assertNotNull(result);
        assertEquals("parent-name", result.getName());
        assertNotNull(result.getChild());
        assertEquals(childId, result.getChild().getId());
        assertEquals("child-label", result.getChild().getLabel());
    }

    @Test
    void convertToTargetClassToCompositeClass_withNullValue_shouldReturnNull() throws SpeedyHttpException {
        // Act
        TestEntity result = SpeedySerializer.toJavaObject(null, TestEntity.class);

        // Assert
        assertNull(result);
    }

    @Test
    void convertToTargetClassToCompositeClass_withNonEntityValue_shouldThrowException() throws SpeedyHttpException {
        // Arrange
        EntityMetadata entityMetadata = StaticEntityMetadata.createEntityMetadata(TestEntity.class);
        SpeedyEntity entity = new SpeedyEntity(entityMetadata);

        // Act & Assert
        SpeedySerializer.toJavaObject(entity, TestEntity.class);
    }

    @Test
    void convertToClass_withBasicTypes_shouldToJavaFieldCorrectly() throws SpeedyHttpException {
        // Act & Assert
        assertEquals("test", SpeedySerializer.asJavaObject(new SpeedyText("test")));
        assertEquals(Long.valueOf(42L), SpeedySerializer.asJavaObject(new SpeedyInt(42L)));
        assertEquals(true, SpeedySerializer.asJavaObject(new SpeedyBoolean(true)));
    }

    @Getter
    @Setter
    // Test composite class with all supported types
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
        private Instant instant;
        private Timestamp timestamp;
        private ZonedDateTime zonedDateTime;
        private Date utilDate;
        private java.sql.Date sqlDate;

        // Collection type
        private java.util.List<String> tags;
    }

    @Getter
    @Setter
    public static class ChildEntity {
        @jakarta.persistence.Id
        private java.util.UUID id;
        private String label;
    }

    @Getter
    @Setter
    public static class ParentEntity {
        @jakarta.persistence.Id
        private java.util.UUID id;
        private String name;
        @jakarta.persistence.OneToOne
        private ChildEntity child;
    }
}
