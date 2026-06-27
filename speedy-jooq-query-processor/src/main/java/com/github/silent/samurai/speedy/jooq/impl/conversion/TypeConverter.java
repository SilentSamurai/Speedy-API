package com.github.silent.samurai.speedy.jooq.impl.conversion;

import com.github.silent.samurai.speedy.conversion.codec.Codec;
import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.EnumMode;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.jooq.impl.Dialects;
import com.github.silent.samurai.speedy.jooq.impl.dialect.DefaultDialect;
import com.github.silent.samurai.speedy.models.*;
import org.jooq.SQLDialect;

/// Converts between JDBC column values and {@link SpeedyValue} by routing on
/// {@link ValueType} and {@link ColumnType} and delegating the actual
/// type conversions to codecs looked up from a {@link DefaultDialect}.
///
/// This is the DB-side walker, analogous to {@link com.github.silent.samurai.speedy.conversion.walker.java.JavaToSpeedy}
/// and {@link com.github.silent.samurai.speedy.conversion.walker.java.SpeedyToJava} for the Java side.
/// The dialect-dependent choice of encode carrier type and codec set is owned by the
/// {@link DefaultDialect}, so this walker contains no dialect branching.
///
/// @see DefaultDialect
public class TypeConverter {

    private final DefaultDialect dialect;

    public TypeConverter(DefaultDialect dialect) {
        this.dialect = dialect;
    }

    /// Dialect-agnostic default converter ({@link SQLDialect#DEFAULT}). Suitable for tests that don't
    /// exercise dialect-specific encoding; production wiring passes the real dialect.
    public static TypeConverter defaults() {
        return defaults(SQLDialect.DEFAULT);
    }

    /// Converter for {@code dialect}, so encoded column values are dialect-correct (e.g.
    /// {@code LocalDateTime} for zoned timestamps on MySQL/MariaDB).
    public static TypeConverter defaults(SQLDialect dialect) {
        return new TypeConverter(Dialects.forJooq(dialect));
    }

    private Object encode(ColumnType col, SpeedyValue sv) {
        if (sv == null || sv instanceof SpeedyNull) return null;
        Class<?> targetType = dialect.encodeCarrier(col);
        if (targetType == null) return null;
        Codec<?> codec = dialect.findCodec(col, targetType);
        if (codec != null) {
            return codec.safeEncode(sv);
        }
        return null;
    }

    private SpeedyValue decodeOrThrow(ColumnType columnType, Object instance) {
        Codec<?> codec = dialect.findCodec(columnType, instance.getClass());
        if (codec == null) {
            throw new RuntimeException("No codec registered for " + columnType + " from " + instance.getClass());
        }
        return codec.safeDecode(instance);
    }

    private Object encodeOrThrow(ColumnType columnType, SpeedyValue speedyValue) {
        Class<?> targetType = dialect.encodeCarrier(columnType);
        if (targetType == null) {
            throw new RuntimeException("No default encode type for " + columnType);
        }
        Codec<?> codec = dialect.findCodec(columnType, targetType);
        if (codec == null) {
            throw new RuntimeException("No codec registered for " + columnType + " -> " + targetType);
        }
        return codec.safeEncode(speedyValue);
    }

    public SpeedyValue toSpeedyValue(Object instance, FieldMetadata fieldMetadata) throws SpeedyHttpException {
        if (instance == null) {
            return SpeedyNull.SPEEDY_NULL;
        }

        ColumnType columnType = fieldMetadata.getColumnType();
        ValueType valueType = fieldMetadata.getValueType();

        return switch (valueType) {
            case BOOL -> new SpeedyBoolean((Boolean) instance);
            case TEXT -> {
                Codec<?> codec = dialect.findCodec(columnType, instance.getClass());
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
                Codec<?> codec = dialect.findCodec(columnType, instance.getClass());
                if (codec != null) {
                    yield codec.safeDecode(instance);
                }
                if (instance instanceof Number n) {
                    yield new SpeedyInt(n.longValue());
                }
                throw new RuntimeException("Unsupported INT java type: " + instance.getClass());
            }
            case FLOAT -> {
                Codec<?> codec = dialect.findCodec(columnType, instance.getClass());
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
