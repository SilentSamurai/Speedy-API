package com.github.silent.samurai.speedy.query.jooq;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyDouble;
import com.github.silent.samurai.speedy.data.StaticFieldMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that {@link JooqConversionImpl} correctly converts DECIMAL/NUMERIC JDBC values
 * supplied as {@link BigDecimal} into {@link SpeedyDouble}.
 */
class JooqBigDecimalConversionTest {

    private JooqConversionImpl conversion;
    private FieldMetadata decimalFloatMetadata;

    /**
     * Dummy entity backing field for {@link StaticFieldMetadata}.
     */
    static class DummyEntity { private BigDecimal dummyDecimal; }

    /**
     * FieldMetadata enforcing DECIMAL / FLOAT mapping.
     */
    static class DecimalFieldMetadata extends StaticFieldMetadata {
        DecimalFieldMetadata() {
            try {
                setField(DummyEntity.class.getDeclaredField("dummyDecimal"));
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }
        @Override
        public ColumnType getColumnType() { return ColumnType.DECIMAL; }
        @Override
        public ValueType getValueType() { return ValueType.FLOAT; }
    }

    @BeforeEach
    void setUp() {
        conversion = new JooqConversionImpl();
        decimalFloatMetadata = new DecimalFieldMetadata();
    }

    @Test
    void bigDecimalValueReturnedForDecimal_shouldConvertToSpeedyDouble() throws SpeedyHttpException {
        BigDecimal jdbcValue = new BigDecimal("12345.6789");
        SpeedyValue speedyValue = conversion.toSpeedyValue(jdbcValue, decimalFloatMetadata);

        assertInstanceOf(SpeedyDouble.class, speedyValue, "Expected SpeedyDouble for BigDecimal DECIMAL value");
        assertEquals(jdbcValue.doubleValue(), speedyValue.asDouble(), 1e-6);
    }
}
