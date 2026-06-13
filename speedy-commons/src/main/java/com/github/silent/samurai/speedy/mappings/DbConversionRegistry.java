package com.github.silent.samurai.speedy.mappings;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.EnumMode;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.Converter;
import com.github.silent.samurai.speedy.models.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/// Converts between JDBC column values and {@link SpeedyValue} using a
/// compound key of {@code (ColumnType, Class<?>)}.
///
/// ## Design Philosophy
/// Like {@link JsonRegistry}, this registry stores codecs under a compound key:
/// the column type ({@link ColumnType}) **and** the Java class of the JDBC value.
/// Each codec handles exactly one Java class — no {@code instanceof} branching
/// inside decode lambdas.
///
/// The {@link #toSpeedyValue(Object, FieldMetadata)} and
/// {@link #toColumnType(SpeedyValue, FieldMetadata)} methods from the
/// {@link Converter} interface handle the higher-level routing (enum modes,
/// special-case column handling) and delegate to codecs for the actual
/// type conversions.
///
/// @see Codec
/// @see Converter
public class DbConversionRegistry implements Converter {

    private final DbConversionRegistry parent;
    private final Map<ColumnType, Map<Class<?>, Codec<?>>> codecs = new HashMap<>();

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

    public DbConversionRegistry() {
        this.parent = null;
    }

    public DbConversionRegistry(DbConversionRegistry parent) {
        this.parent = parent;
    }

    /// Registers a codec that converts between a single Java class `T` and a
    /// SpeedyValue for the given {@link ColumnType}.
    ///
    /// @param col    the column type
    /// @param type   the Java class handled by this codec
    /// @param encode {@code SpeedyValue → T}
    /// @param decode {@code T → SpeedyValue}
    public <T> DbConversionRegistry register(ColumnType col, Class<T> type,
                                             Function<SpeedyValue, T> encode,
                                             Function<T, SpeedyValue> decode) {
        codecs.computeIfAbsent(col, k -> new HashMap<>()).put(type, new Codec<>(type, encode, decode));
        return this;
    }

    /// Looks up a codec by ColumnType and Java class, walking supertypes if no
    /// exact match is found.
    private Codec<?> findCodec(ColumnType col, Class<?> clazz) {
        Map<Class<?>, Codec<?>> byClass = codecs.get(col);
        if (byClass != null) {
            Codec<?> c = byClass.get(clazz);
            if (c != null) return c;
            for (Map.Entry<Class<?>, Codec<?>> e : byClass.entrySet()) {
                if (e.getKey().isAssignableFrom(clazz)) return e.getValue();
            }
        }
        if (parent != null) return parent.findCodec(col, clazz);
        return null;
    }

    /// Encodes a SpeedyValue into its default JDBC type.
    private Object encode(ColumnType col, SpeedyValue sv) {
        if (sv == null || sv instanceof SpeedyNull) return null;
        Class<?> targetType = DEFAULT_ENCODE_TYPE.get(col);
        if (targetType == null) return null;
        Codec<?> codec = findCodec(col, targetType);
        if (codec != null) {
            return codec.safeEncode(sv);
        }
        return null;
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
                Codec<?> codec = findCodec(columnType, instance.getClass());
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
                Codec<?> codec = findCodec(columnType, instance.getClass());
                if (codec != null) {
                    yield codec.safeDecode(instance);
                }
                if (instance instanceof Number n) {
                    yield new SpeedyInt(n.longValue());
                }
                throw new RuntimeException("Unsupported INT java type: " + instance.getClass());
            }
            case FLOAT -> {
                Codec<?> codec = findCodec(columnType, instance.getClass());
                if (codec != null) {
                    yield (SpeedyDouble) codec.safeDecode(instance);
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

    private SpeedyValue decodeOrThrow(ColumnType columnType, Object instance) {
        Codec<?> codec = findCodec(columnType, instance.getClass());
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
        Codec<?> codec = findCodec(columnType, targetType);
        if (codec == null) {
            throw new RuntimeException("No codec registered for " + columnType + " -> " + targetType);
        }
        return codec.safeEncode(speedyValue);
    }

    public static DbConversionRegistry defaults() {
        DbConversionRegistry r = new DbConversionRegistry();
        // Defaults registered by JooqConverters
        return r;
    }
}
