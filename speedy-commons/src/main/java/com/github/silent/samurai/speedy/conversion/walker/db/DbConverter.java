package com.github.silent.samurai.speedy.conversion.walker.db;

import com.github.silent.samurai.speedy.conversion.codec.Codec;
import com.github.silent.samurai.speedy.conversion.registry.DbConversionRegistry;
import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.EnumMode;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.Converter;
import com.github.silent.samurai.speedy.models.*;

import java.util.Map;

/// Converts between JDBC column values and {@link SpeedyValue} by routing on
/// {@link ValueType} and {@link ColumnType} and delegating the actual
/// type conversions to codecs looked up from a {@link DbConversionRegistry}.
///
/// This is the DB-side walker, analogous to {@link com.github.silent.samurai.speedy.conversion.walker.java.JavaToSpeedy}
/// and {@link com.github.silent.samurai.speedy.conversion.walker.java.SpeedyToJava} for the Java side.
///
/// @see DbConversionRegistry
/// @see Converter
public class DbConverter implements Converter {

    /// Default target Java class when encoding a SpeedyValue for a ColumnType.
    private static final Map<ColumnType, Class<?>> DEFAULT_ENCODE_TYPE = Map.ofEntries(
            Map.entry(ColumnType.DATE, java.sql.Date.class),
            Map.entry(ColumnType.TIME, java.sql.Time.class),
            Map.entry(ColumnType.TIMESTAMP, java.sql.Timestamp.class),
            Map.entry(ColumnType.TIMESTAMP_WITH_ZONE, java.time.OffsetDateTime.class),
            Map.entry(ColumnType.NUMERIC, java.math.BigDecimal.class),
            Map.entry(ColumnType.DECIMAL, java.math.BigDecimal.class),
            Map.entry(ColumnType.REAL, Double.class),
            Map.entry(ColumnType.BIGINT, java.math.BigInteger.class),
            Map.entry(ColumnType.TEXT, String.class),
            Map.entry(ColumnType.CLOB, String.class),
            Map.entry(ColumnType.VARCHAR, String.class),
            Map.entry(ColumnType.CHAR, String.class),
            Map.entry(ColumnType.UUID, java.util.UUID.class)
    );

    private final DbConversionRegistry registry;

    public DbConverter(DbConversionRegistry registry) {
        this.registry = registry;
    }

    private Object encode(ColumnType col, SpeedyValue sv) {
        if (sv == null || sv instanceof SpeedyNull) return null;
        Class<?> targetType = DEFAULT_ENCODE_TYPE.get(col);
        if (targetType == null) return null;
        Codec<?> codec = registry.findCodec(col, targetType);
        if (codec != null) {
            return codec.safeEncode(sv);
        }
        return null;
    }

    private SpeedyValue decodeOrThrow(ColumnType columnType, Object instance) {
        Codec<?> codec = registry.findCodec(columnType, instance.getClass());
        if (codec == null) {
            throw new RuntimeException("No codec registered for " + columnType + " from " + instance.getClass());
        }
        return codec.safeDecode(instance);
    }

    private Object encodeOrThrow(ColumnType columnType, SpeedyValue speedyValue) {
        Class<?> targetType = DEFAULT_ENCODE_TYPE.get(columnType);
        if (targetType == null) {
            throw new RuntimeException("No default encode type for " + columnType);
        }
        Codec<?> codec = registry.findCodec(columnType, targetType);
        if (codec == null) {
            throw new RuntimeException("No codec registered for " + columnType + " -> " + targetType);
        }
        return codec.safeEncode(speedyValue);
    }

    @Override
    public SpeedyValue toSpeedyValue(Object instance, FieldMetadata fieldMetadata) throws SpeedyHttpException {
        if (instance == null) {
            return SpeedyNull.SPEEDY_NULL;
        }

        ColumnType columnType = fieldMetadata.getColumnType();
        ValueType valueType = fieldMetadata.getValueType();

        return switch (valueType) {
            case BOOL -> new SpeedyBoolean((Boolean) instance);
            case TEXT -> {
                Codec<?> codec = registry.findCodec(columnType, instance.getClass());
                if (codec != null) {
                    yield codec.safeDecode(instance);
                }
                yield new SpeedyText(String.valueOf(instance));
            }
            case ENUM -> {
                try {
                    if (fieldMetadata.getStoredEnumMode() == EnumMode.ORDINAL) {
                        int ord = instance instanceof Number n ? n.intValue()
                                : Integer.parseInt(String.valueOf(instance));
                        String name = fieldMetadata.getDynamicEnum()
                                .fromCode(ord)
                                .orElseThrow(() -> new RuntimeException(
                                        "DB contains unknown enum ordinal " + ord
                                                + " for field " + fieldMetadata.getOutputPropertyName()))
                                .name();
                        yield new SpeedyEnum(name, fieldMetadata);
                    }
                    yield new SpeedyEnum(String.valueOf(instance), fieldMetadata);
                } catch (SpeedyHttpException e) {
                    throw new RuntimeException(e);
                }
            }
            case ENUM_ORD -> {
                try {
                    if (fieldMetadata.getStoredEnumMode() == EnumMode.STRING) {
                        String name = String.valueOf(instance);
                        long code = fieldMetadata.getDynamicEnum()
                                .fromName(name)
                                .orElseThrow(() -> new RuntimeException(
                                        "DB contains unknown enum name '" + name
                                                + "' for field " + fieldMetadata.getOutputPropertyName()))
                                .code();
                        yield new SpeedyEnum(code, fieldMetadata);
                    }
                    if (instance instanceof Number n) {
                        yield new SpeedyEnum(n.longValue(), fieldMetadata);
                    }
                    yield new SpeedyEnum(Long.parseLong(String.valueOf(instance)), fieldMetadata);
                } catch (SpeedyHttpException e) {
                    throw new RuntimeException(e);
                }
            }
            case INT -> {
                Codec<?> codec = registry.findCodec(columnType, instance.getClass());
                if (codec != null) {
                    yield codec.safeDecode(instance);
                }
                if (instance instanceof Number n) {
                    yield new SpeedyInt(n.longValue());
                }
                throw new RuntimeException("Unsupported INT java type: " + instance.getClass());
            }
            case FLOAT -> {
                Codec<?> codec = registry.findCodec(columnType, instance.getClass());
                if (codec != null) {
                    yield codec.safeDecode(instance);
                }
                if (instance instanceof Number n) {
                    yield new SpeedyDouble(n.doubleValue());
                }
                throw new RuntimeException("Unsupported FLOAT java type: " + instance.getClass());
            }
            case DATE -> decodeOrThrow(columnType, instance);
            case TIME -> decodeOrThrow(columnType, instance);
            case DATE_TIME -> decodeOrThrow(columnType, instance);
            case ZONED_DATE_TIME -> decodeOrThrow(columnType, instance);
            case OBJECT, COLLECTION ->
                    throw new RuntimeException("Cannot convert container types from column value");
            case NULL -> SpeedyNull.SPEEDY_NULL;
        };
    }

    @Override
    public Object toColumnType(SpeedyValue speedyValue, FieldMetadata fieldMetadata) {
        ColumnType columnType = fieldMetadata.getColumnType();
        return switch (speedyValue.getValueType()) {
            case BOOL -> switch (columnType) {
                case BOOLEAN -> speedyValue.asBoolean();
                default -> throw new RuntimeException("Unsupported column type: " + columnType);
            };
            case TEXT -> {
                Object encoded = encode(columnType, speedyValue);
                if (encoded != null) yield encoded;
                yield ((SpeedyText) speedyValue).asText();
            }
            case ENUM -> switch (fieldMetadata.getStoredEnumMode()) {
                case STRING -> {
                    SpeedyText st = new SpeedyText(speedyValue.asEnum());
                    Object encoded = encode(columnType, st);
                    if (encoded != null) yield encoded;
                    yield st.asText();
                }
                case ORDINAL -> {
                    DynamicEnum.Variant v = fieldMetadata.getDynamicEnum()
                            .fromName(speedyValue.asEnum())
                            .orElseThrow(() -> new RuntimeException("Enum value '" + speedyValue.asEnum() + "' is not valid."));
                    SpeedyInt speedyInt = new SpeedyInt((long) v.code());
                    yield switch (columnType) {
                        case INTEGER, SMALLINT -> speedyInt.asInt();
                        case BIGINT -> {
                            Object encoded = encode(columnType, speedyInt);
                            yield encoded != null ? encoded : speedyInt.asLong();
                        }
                        default ->
                                throw new RuntimeException("Enum stored as ORDINAL cannot be written to column type: " + columnType);
                    };
                }
            };
            case ENUM_ORD -> switch (fieldMetadata.getStoredEnumMode()) {
                case STRING -> {
                    DynamicEnum.Variant v = fieldMetadata.getDynamicEnum()
                            .fromCode(speedyValue.asEnumOrd().intValue())
                            .orElseThrow(() -> new RuntimeException("Enum ordinal value '" + speedyValue.asEnumOrd() + "' is not valid."));
                    SpeedyText st = new SpeedyText(v.name());
                    Object encoded = encode(columnType, st);
                    yield encoded != null ? encoded : st.asText();
                }
                case ORDINAL -> {
                    SpeedyInt speedyInt = new SpeedyInt(speedyValue.asEnumOrd());
                    yield switch (columnType) {
                        case INTEGER, SMALLINT -> speedyInt.asInt();
                        case BIGINT -> {
                            Object encoded = encode(columnType, speedyInt);
                            yield encoded != null ? encoded : speedyInt.asLong();
                        }
                        default ->
                                throw new RuntimeException("Enum stored as ORDINAL cannot be written to column type: " + columnType);
                    };
                }
            };
            case INT -> {
                SpeedyInt speedyInt = (SpeedyInt) speedyValue;
                Object encoded = encode(columnType, speedyInt);
                if (encoded != null) yield encoded;
                yield switch (columnType) {
                    case INTEGER, SMALLINT -> speedyInt.asInt();
                    case BIGINT -> speedyInt.asLong();
                    default -> throw new RuntimeException("Unsupported column type: " + columnType);
                };
            }
            case FLOAT -> {
                SpeedyDouble speedyDouble = (SpeedyDouble) speedyValue;
                Object encoded = encode(columnType, speedyDouble);
                if (encoded != null) yield encoded;
                yield switch (columnType) {
                    case FLOAT -> speedyDouble.asDouble().floatValue();
                    case DOUBLE -> speedyDouble.asDouble();
                    default -> throw new RuntimeException("Unsupported column type: " + columnType);
                };
            }
            case DATE -> encodeOrThrow(columnType, (SpeedyDate) speedyValue);
            case TIME -> encodeOrThrow(columnType, (SpeedyTime) speedyValue);
            case DATE_TIME -> encodeOrThrow(columnType, (SpeedyDateTime) speedyValue);
            case ZONED_DATE_TIME -> encodeOrThrow(columnType, (SpeedyZonedDateTime) speedyValue);
            case OBJECT, COLLECTION ->
                    throw new RuntimeException("Cannot convert from " + speedyValue + " to " + columnType);
            case NULL -> null;
        };
    }
}
