package com.github.silent.samurai.speedy.query.jooq;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyInt;
import com.github.silent.samurai.speedy.data.StaticFieldMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that {@link JooqConversionImpl} correctly converts BIGINT JDBC values
 * whether provided as {@link Long} or {@link BigInteger} into {@link SpeedyInt}.
 */
class JooqBigIntConversionTest {

    private JooqConversionImpl conversion;
    private FieldMetadata bigintIntMetadata;

    /**
     * Dummy holder class to back the {@link StaticFieldMetadata} instance.
     */
    static class DummyEntity { private Long dummyLong; }

    /**
     * FieldMetadata that delegates to {@link StaticFieldMetadata} but forces BIGINT / INT mapping.
     */
    static class BigintFieldMetadata extends StaticFieldMetadata {
        BigintFieldMetadata() {
            try {
                setField(DummyEntity.class.getDeclaredField("dummyLong"));
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }
        @Override
        public ColumnType getColumnType() { return ColumnType.BIGINT; }
        @Override
        public ValueType getValueType() { return ValueType.INT; }
    }

    @BeforeEach
    void setUp() {
        conversion = new JooqConversionImpl();
        bigintIntMetadata = new BigintFieldMetadata();
    }

    @Test
    void longValueReturnedForBigint_shouldConvertToSpeedyInt() throws SpeedyHttpException {
        long jdbcValue = 123456789L; // typical value returned by driver
        SpeedyValue speedyValue = conversion.toSpeedyValue(jdbcValue, bigintIntMetadata);

        assertInstanceOf(SpeedyInt.class, speedyValue, "Expected SpeedyInt for LONG BIGINT value");
        assertEquals(jdbcValue, speedyValue.asLong());
    }

    @Test
    void bigIntegerValueReturnedForBigint_shouldConvertViaFromBigInt() throws SpeedyHttpException {
        BigInteger jdbcValue = new BigInteger("9876543210");
        SpeedyValue speedyValue = conversion.toSpeedyValue(jdbcValue, bigintIntMetadata);

        assertInstanceOf(SpeedyInt.class, speedyValue, "Expected SpeedyInt for BigInteger BIGINT value");
        assertEquals(jdbcValue.longValue(), speedyValue.asLong());
    }
}
