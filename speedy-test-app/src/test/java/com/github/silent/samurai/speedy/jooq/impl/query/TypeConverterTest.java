package com.github.silent.samurai.speedy.jooq.impl.query;

import com.github.silent.samurai.speedy.data.Product;
import com.github.silent.samurai.speedy.data.StaticEntityMetadata;
import com.github.silent.samurai.speedy.data.StaticFieldMetadata;
import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.EnumMode;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.jooq.impl.conversion.TypeConverter;
import com.github.silent.samurai.speedy.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TypeConverterTest {

    private TypeConverter converter;

    enum TestEnum { ALPHA, BETA, GAMMA }

    @BeforeEach
    void setUp() {
        converter = TypeConverter.defaults();
    }

    static class CustomField extends StaticFieldMetadata {
        private final ColumnType columnType;
        private final ValueType valueType;
        private final EnumMode storedEnumMode;
        private final EnumMode operationalEnumMode;
        private final boolean isEnum;

        CustomField(ColumnType ct, ValueType vt) {
            this(ct, vt, null, null, false);
        }

        CustomField(ColumnType ct, ValueType vt, EnumMode storedEm, EnumMode operationalEm, boolean isEnum) {
            this.columnType = ct;
            this.valueType = vt;
            this.storedEnumMode = storedEm;
            this.operationalEnumMode = operationalEm;
            this.isEnum = isEnum;
            try {
                setField(Product.class.getDeclaredField("id"));
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }

        @Override public ColumnType getColumnType() { return columnType; }
        @Override public ValueType getValueType() { return valueType; }
        @Override public EnumMode getStoredEnumMode() { return storedEnumMode != null ? storedEnumMode : EnumMode.STRING; }
        @Override public EnumMode getOperationalEnumMode() { return operationalEnumMode != null ? operationalEnumMode : getStoredEnumMode(); }
        @Override public DynamicEnum getDynamicEnum() { return DynamicEnum.of(TestEnum.class); }
        @Override public boolean isEnum() { return isEnum; }
    }

    // ---- toSpeedyValue: JDBC column value → SpeedyValue ----

    @Test
    void toSpeedyValue_nullInstance_returnsSpeedyNull() throws SpeedyHttpException {
        // Covers the null-guard early-return at the top of toSpeedyValue
        FieldMetadata fm = new CustomField(ColumnType.TEXT, ValueType.TEXT);
        SpeedyValue result = converter.toSpeedyValue(null, fm);
        assertTrue(result.isNull());
    }

    @Test
    void toSpeedyValue_textNoCodec_fallsBackToSpeedyText() throws SpeedyHttpException {
        // ValueType TEXT with a column type that has no registered codec — falls through to String.valueOf
        FieldMetadata fm = new CustomField(ColumnType.BLOB, ValueType.TEXT);
        SpeedyValue result = converter.toSpeedyValue(new Object(), fm);
        assertInstanceOf(SpeedyText.class, result);
    }

    @Test
    void toSpeedyValue_enumOrdinal_numberInstance() throws SpeedyHttpException {
        // ValueType ENUM with stored ORDINAL, instance is Number — resolves ordinal via DynamicEnum.fromCode
        FieldMetadata fm = new CustomField(ColumnType.INTEGER, ValueType.ENUM, EnumMode.ORDINAL, EnumMode.STRING, true);
        SpeedyValue result = converter.toSpeedyValue(0, fm);
        assertInstanceOf(SpeedyEnum.class, result);
        assertEquals("ALPHA", result.asEnum());
    }

    @Test
    void toSpeedyValue_enumOrdinal_stringInstance() throws SpeedyHttpException {
        // ValueType ENUM with stored ORDINAL, instance is String — parsed via Integer.parseInt then fromCode
        FieldMetadata fm = new CustomField(ColumnType.INTEGER, ValueType.ENUM, EnumMode.ORDINAL, EnumMode.STRING, true);
        SpeedyValue result = converter.toSpeedyValue("1", fm);
        assertInstanceOf(SpeedyEnum.class, result);
        assertEquals("BETA", result.asEnum());
    }

    @Test
    void toSpeedyValue_enumOrd_stringStored() throws SpeedyHttpException {
        // ValueType ENUM_ORD with stored STRING — resolves name via DynamicEnum.fromName, returns ordinal code
        FieldMetadata fm = new CustomField(ColumnType.VARCHAR, ValueType.ENUM_ORD, EnumMode.STRING, EnumMode.ORDINAL, true);
        SpeedyValue result = converter.toSpeedyValue("ALPHA", fm);
        assertInstanceOf(SpeedyEnum.class, result);
        assertEquals(0L, result.asEnumOrd());
    }

    @Test
    void toSpeedyValue_enumOrd_ordinalStored_number() throws SpeedyHttpException {
        // ValueType ENUM_ORD with stored ORDINAL, instance is Number — uses longValue directly
        FieldMetadata fm = new CustomField(ColumnType.INTEGER, ValueType.ENUM_ORD, EnumMode.ORDINAL, EnumMode.ORDINAL, true);
        SpeedyValue result = converter.toSpeedyValue(2L, fm);
        assertInstanceOf(SpeedyEnum.class, result);
        assertEquals(2L, result.asEnumOrd());
    }

    @Test
    void toSpeedyValue_enumOrd_ordinalStored_string() throws SpeedyHttpException {
        // ValueType ENUM_ORD with stored ORDINAL, instance is String — parsed via Long.parseLong
        FieldMetadata fm = new CustomField(ColumnType.INTEGER, ValueType.ENUM_ORD, EnumMode.ORDINAL, EnumMode.ORDINAL, true);
        SpeedyValue result = converter.toSpeedyValue("2", fm);
        assertInstanceOf(SpeedyEnum.class, result);
        assertEquals(2L, result.asEnumOrd());
    }

    @Test
    void toSpeedyValue_intFallback_usesSpeedyInt() throws SpeedyHttpException {
        // ValueType INT with no codec registered for this column type — falls through to new SpeedyInt(n.longValue())
        FieldMetadata fm = new CustomField(ColumnType.SMALLINT, ValueType.INT);
        SpeedyValue result = converter.toSpeedyValue(42, fm);
        assertInstanceOf(SpeedyInt.class, result);
        assertEquals(42L, result.asLong());
    }

    @Test
    void toSpeedyValue_intFallback_nonNumber_throws() {
        // ValueType INT with no codec and instance is not a Number — RuntimeException
        FieldMetadata fm = new CustomField(ColumnType.SMALLINT, ValueType.INT);
        assertThrows(RuntimeException.class, () -> converter.toSpeedyValue("not-a-number", fm));
    }

    @Test
    void toSpeedyValue_floatFallback_usesSpeedyDouble() throws SpeedyHttpException {
        // ValueType FLOAT with no codec registered — falls through to new SpeedyDouble(n.doubleValue())
        FieldMetadata fm = new CustomField(ColumnType.REAL, ValueType.FLOAT);
        SpeedyValue result = converter.toSpeedyValue(3.14d, fm);
        assertInstanceOf(SpeedyDouble.class, result);
        assertEquals(3.14d, result.asDouble(), 1e-9);
    }

    @Test
    void toSpeedyValue_floatFallback_nonNumber_throws() {
        // ValueType FLOAT with no codec and instance is not a Number — RuntimeException
        FieldMetadata fm = new CustomField(ColumnType.REAL, ValueType.FLOAT);
        assertThrows(RuntimeException.class, () -> converter.toSpeedyValue("not-a-number", fm));
    }

    @Test
    void toSpeedyValue_objectValueType_throws() {
        // ValueType OBJECT / COLLECTION are not decodable from a column value
        FieldMetadata fm = new CustomField(ColumnType.TEXT, ValueType.OBJECT);
        assertThrows(RuntimeException.class, () -> converter.toSpeedyValue(new Object(), fm));
    }

    @Test
    void toSpeedyValue_nullValueType_returnsSpeedyNull() throws SpeedyHttpException {
        // ValueType NULL — returns SpeedyNull regardless of input
        FieldMetadata fm = new CustomField(ColumnType.TEXT, ValueType.NULL);
        SpeedyValue result = converter.toSpeedyValue("anything", fm);
        assertTrue(result.isNull());
    }

    // ---- toColumnType: SpeedyValue → JDBC column value ----

    @Test
    void toColumnType_boolToNonBooleanCol_throws() {
        // BOOL value type with a non-BOOLEAN column type triggers the default throw
        FieldMetadata fm = new CustomField(ColumnType.INTEGER, ValueType.BOOL);
        assertThrows(Exception.class, () -> converter.toColumnType(new SpeedyBoolean(true), fm));
    }

    @Test
    void toColumnType_textEncodingNull_fallsBackToAsText() throws Exception {
        // TEXT value type with a column type that has no encodeCarrier → encode returns null → fallback to asText()
        FieldMetadata fm = new CustomField(ColumnType.BLOB, ValueType.TEXT);
        Object result = converter.toColumnType(new SpeedyText("hello"), fm);
        assertEquals("hello", result);
    }

    @Test
    void toColumnType_enumToStringStored_encodeNull_fallsBackToAsText() throws Exception {
        // ENUM, stored STRING, column type without encodeCarrier → encode null → fallback to asText()
        FieldMetadata fm = new CustomField(ColumnType.BLOB, ValueType.ENUM, EnumMode.STRING, EnumMode.STRING, true);
        Object result = converter.toColumnType(new SpeedyEnum("ALPHA", fm), fm);
        assertEquals("ALPHA", result);
    }

    @Test
    void toColumnType_enumToOrdinalStored_integerCol() throws Exception {
        // ENUM, stored ORDINAL, INTEGER column — converts name → ordinal code → asInt()
        FieldMetadata fm = new CustomField(ColumnType.INTEGER, ValueType.ENUM, EnumMode.ORDINAL, EnumMode.STRING, true);
        Object result = converter.toColumnType(new SpeedyEnum("BETA", fm), fm);
        assertInstanceOf(Number.class, result);
        assertEquals(1, ((Number) result).intValue());
    }

    @Test
    void toColumnType_enumToOrdinalStored_smallintCol() throws Exception {
        // ENUM, stored ORDINAL, SMALLINT column — same code path as INTEGER via asInt()
        FieldMetadata fm = new CustomField(ColumnType.SMALLINT, ValueType.ENUM, EnumMode.ORDINAL, EnumMode.STRING, true);
        Object result = converter.toColumnType(new SpeedyEnum("BETA", fm), fm);
        assertInstanceOf(Number.class, result);
        assertEquals(1, ((Number) result).intValue());
    }

    @Test
    void toColumnType_enumToOrdinalStored_bigintCol() throws Exception {
        // ENUM, stored ORDINAL, BIGINT column — uses the codec-backed encode path (BigInteger)
        FieldMetadata fm = new CustomField(ColumnType.BIGINT, ValueType.ENUM, EnumMode.ORDINAL, EnumMode.STRING, true);
        Object result = converter.toColumnType(new SpeedyEnum("BETA", fm), fm);
        assertInstanceOf(Number.class, result);
        assertEquals(1L, ((Number) result).longValue());
    }

    @Test
    void toColumnType_enumToOrdinalStored_unsupportedCol_throws() throws Exception {
        // ENUM, stored ORDINAL, unsupported column type (BLOB) — default throw in inner switch
        FieldMetadata fm = new CustomField(ColumnType.BLOB, ValueType.ENUM, EnumMode.ORDINAL, EnumMode.STRING, true);
        assertThrows(Exception.class, () -> converter.toColumnType(new SpeedyEnum("BETA", fm), fm));
    }

    @Test
    void toColumnType_enumOrdToStringStored() throws Exception {
        // ENUM_ORD, stored STRING — converts ordinal → fromCode → variant name
        FieldMetadata fm = new CustomField(ColumnType.VARCHAR, ValueType.ENUM_ORD, EnumMode.STRING, EnumMode.ORDINAL, true);
        SpeedyEnum value = new SpeedyEnum(0L, fm);
        Object result = converter.toColumnType(value, fm);
        assertEquals("ALPHA", result);
    }

    @Test
    void toColumnType_enumOrdToOrdinalStored_integerCol() throws Exception {
        // ENUM_ORD, stored ORDINAL, SMALLINT column — ordinal directly via asInt()
        FieldMetadata fm = new CustomField(ColumnType.SMALLINT, ValueType.ENUM_ORD, EnumMode.ORDINAL, EnumMode.ORDINAL, true);
        SpeedyEnum value = new SpeedyEnum(1L, fm);
        Object result = converter.toColumnType(value, fm);
        assertInstanceOf(Number.class, result);
        assertEquals(1, ((Number) result).intValue());
    }

    @Test
    void toColumnType_enumOrdToOrdinalStored_bigintCol() throws Exception {
        // ENUM_ORD, stored ORDINAL, BIGINT column — uses the codec-backed encode path
        FieldMetadata fm = new CustomField(ColumnType.BIGINT, ValueType.ENUM_ORD, EnumMode.ORDINAL, EnumMode.ORDINAL, true);
        SpeedyEnum value = new SpeedyEnum(1L, fm);
        Object result = converter.toColumnType(value, fm);
        assertInstanceOf(Number.class, result);
        assertEquals(1L, ((Number) result).longValue());
    }

    @Test
    void toColumnType_enumOrdToOrdinalStored_unsupportedCol_throws() throws Exception {
        // ENUM_ORD, stored ORDINAL, unsupported column type — default throw in inner switch
        FieldMetadata fm = new CustomField(ColumnType.BLOB, ValueType.ENUM_ORD, EnumMode.ORDINAL, EnumMode.ORDINAL, true);
        SpeedyEnum value = new SpeedyEnum(1L, fm);
        assertThrows(Exception.class, () -> converter.toColumnType(value, fm));
    }

    @Test
    void toColumnType_intEncodeNull_integerCol() throws Exception {
        // INT value type with column type that has no encodeCarrier → encode null → fallback to asInt()
        FieldMetadata fm = new CustomField(ColumnType.SMALLINT, ValueType.INT);
        Object result = converter.toColumnType(new SpeedyInt(42L), fm);
        assertInstanceOf(Number.class, result);
        assertEquals(42, ((Number) result).intValue());
    }

    @Test
    void toColumnType_intEncodeNull_bigintCol() throws Exception {
        // INT value type with BIGINT column → encode returns BigInteger via codec, non-null path
        FieldMetadata fm = new CustomField(ColumnType.BIGINT, ValueType.INT);
        Object result = converter.toColumnType(new SpeedyInt(42L), fm);
        assertInstanceOf(Number.class, result);
        assertEquals(42L, ((Number) result).longValue());
    }

    @Test
    void toColumnType_intEncodeNull_unsupportedCol_throws() throws Exception {
        // INT value type with unsupported column type (BLOB) → default throw in inner switch
        FieldMetadata fm = new CustomField(ColumnType.BLOB, ValueType.INT);
        assertThrows(Exception.class, () -> converter.toColumnType(new SpeedyInt(42L), fm));
    }

    @Test
    void toColumnType_floatEncodeNull_floatCol() throws Exception {
        // FLOAT value type with FLOAT column, no encodeCarrier → encode null → asDouble().floatValue()
        FieldMetadata fm = new CustomField(ColumnType.FLOAT, ValueType.FLOAT);
        Object result = converter.toColumnType(new SpeedyDouble(3.14d), fm);
        assertInstanceOf(Float.class, result);
        assertEquals(3.14f, (Float) result, 1e-6);
    }

    @Test
    void toColumnType_floatEncodeNull_doubleCol() throws Exception {
        // FLOAT value type with DOUBLE column, no encodeCarrier → encode null → asDouble()
        FieldMetadata fm = new CustomField(ColumnType.DOUBLE, ValueType.FLOAT);
        Object result = converter.toColumnType(new SpeedyDouble(3.14d), fm);
        assertEquals(3.14d, result);
    }

    @Test
    void toColumnType_floatEncodeNull_unsupportedCol_throws() throws Exception {
        // FLOAT value type with unsupported column type (BLOB) → default throw in inner switch
        FieldMetadata fm = new CustomField(ColumnType.BLOB, ValueType.FLOAT);
        assertThrows(Exception.class, () -> converter.toColumnType(new SpeedyDouble(3.14d), fm));
    }

    @Test
    void toColumnType_objectValueType_throws() {
        // OBJECT / COLLECTION value types cannot be converted to a JDBC column value
        FieldMetadata fm = new CustomField(ColumnType.TEXT, ValueType.OBJECT);
        assertThrows(Exception.class, () -> converter.toColumnType(
                new SpeedyEntity(StaticEntityMetadata.createEntityMetadata(Product.class)), fm));
    }

    @Test
    void toColumnType_nullValueType_returnsNull() {
        // NULL value type → returns null
        FieldMetadata fm = new CustomField(ColumnType.TEXT, ValueType.NULL);
        assertNull(converter.toColumnType(SpeedyNull.SPEEDY_NULL, fm));
    }

    // ---- encodeOrThrow / decodeOrThrow error paths ----

    @Test
    void toSpeedyValue_noCodecRegistered_throws() {
        // Column type with no registered codec → decodeOrThrow throws RuntimeException
        FieldMetadata fm = new CustomField(ColumnType.BLOB, ValueType.DATE);
        assertThrows(RuntimeException.class, () -> converter.toSpeedyValue(new Object(), fm));
    }

    @Test
    void toColumnType_noEncodeCarrier_throws() {
        // Column type with no default encode carrier → encodeOrThrow throws RuntimeException
        FieldMetadata fm = new CustomField(ColumnType.BLOB, ValueType.DATE);
        assertThrows(RuntimeException.class, () -> converter.toColumnType(new SpeedyDate(java.time.LocalDate.now()), fm));
    }
}
