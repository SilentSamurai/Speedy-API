package com.github.silent.samurai.speedy.mappings;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.EnumMode;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.Converter;
import com.github.silent.samurai.speedy.models.*;

import java.math.BigInteger;

public class DbConversionRegistry extends ConversionRegistry<ColumnType> implements Converter {

    public DbConversionRegistry(DbConversionRegistry parent) {
        super(parent);
    }

    @Override
    public SpeedyValue toSpeedyValue(Object instance, FieldMetadata fieldMetadata) throws SpeedyHttpException {
        if (instance == null) {
            return SpeedyNull.SPEEDY_NULL;
        }

        ColumnType columnType = fieldMetadata.getColumnType();
        ValueType valueType = fieldMetadata.getValueType();

        return switch (valueType) {
            case BOOL:
                if (instance instanceof Boolean b) {
                    yield new SpeedyBoolean(b);
                }
                throw new RuntimeException("Unsupported BOOL java type: " + instance.getClass());
            case TEXT:
                yield convertTextFromColumn(instance, columnType);
            case ENUM:
                // Operational mode is STRING. DB may store ordinal if storedEnumMode == ORDINAL.
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
            case ENUM_ORD:
                // Operational mode is ORDINAL. DB may store the name if storedEnumMode == STRING.
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
            case INT:
                if (columnType == ColumnType.BIGINT && instance instanceof BigInteger) {
                    Codec codec = lookup(columnType);
                    if (codec != null) {
                        yield codec.decode().apply(instance);
                    }
                }
                if (instance instanceof Number n) {
                    yield new SpeedyInt(n.longValue());
                }
                throw new RuntimeException("Unsupported INT java type: " + instance.getClass());
            case FLOAT:
                yield convertFloatFromColumn(instance, columnType);
            case DATE:
                yield decodeOrThrow(columnType, instance);
            case TIME:
                yield decodeOrThrow(columnType, instance);
            case DATE_TIME:
                yield decodeOrThrow(columnType, instance);
            case ZONED_DATE_TIME:
                yield decodeOrThrow(columnType, instance);
            case OBJECT:
            case COLLECTION:
                throw new RuntimeException("Cannot convert container types from column value");
            case NULL:
                yield SpeedyNull.SPEEDY_NULL;
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
            case TEXT -> convertTextToColumn((SpeedyText) speedyValue, columnType);
            case ENUM -> switch (fieldMetadata.getStoredEnumMode()) {
                case STRING -> {
                    SpeedyText st = new SpeedyText(speedyValue.asEnum());
                    yield encodeOrThrow(columnType, st);
                }
                case ORDINAL -> {
                    DynamicEnum.Variant v = fieldMetadata.getDynamicEnum()
                            .fromName(speedyValue.asEnum())
                            .orElseThrow(() -> new RuntimeException("Enum value '" + speedyValue.asEnum() + "' is not valid."));
                    SpeedyInt speedyInt = new SpeedyInt((long) v.code());
                    yield switch (columnType) {
                        case INTEGER, SMALLINT -> speedyInt.asInt();
                        case BIGINT -> encodeOrThrow(columnType, speedyInt);
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
                    yield encodeOrThrow(columnType, st);
                }
                case ORDINAL -> {
                    SpeedyInt speedyInt = new SpeedyInt(speedyValue.asEnumOrd());
                    yield switch (columnType) {
                        case INTEGER, SMALLINT -> speedyInt.asInt();
                        case BIGINT -> encodeOrThrow(columnType, speedyInt);
                        default ->
                                throw new RuntimeException("Enum stored as ORDINAL cannot be written to column type: " + columnType);
                    };
                }
            };
            case INT -> {
                SpeedyInt speedyInt = (SpeedyInt) speedyValue;
                yield switch (columnType) {
                    case INTEGER, SMALLINT -> speedyInt.asInt();
                    case BIGINT -> encodeOrThrow(columnType, speedyInt);
                    default -> throw new RuntimeException("Unsupported column type: " + columnType);
                };
            }
            case FLOAT -> convertFloatToColumn((SpeedyDouble) speedyValue, columnType);
            case DATE -> encodeOrThrow(columnType, (SpeedyDate) speedyValue);
            case TIME -> encodeOrThrow(columnType, (SpeedyTime) speedyValue);
            case DATE_TIME -> encodeOrThrow(columnType, (SpeedyDateTime) speedyValue);
            case ZONED_DATE_TIME -> encodeOrThrow(columnType, (SpeedyZonedDateTime) speedyValue);
            case OBJECT, COLLECTION ->
                    throw new RuntimeException("Cannot convert from " + speedyValue + " to " + columnType);
            case NULL -> null;
        };
    }

    private SpeedyValue convertTextFromColumn(Object instance, ColumnType columnType) {
        Codec codec = lookup(columnType);
        if (codec != null) {
            return codec.decode().apply(instance);
        }
        return new SpeedyText(String.valueOf(instance));
    }

    private SpeedyDouble convertFloatFromColumn(Object instance, ColumnType columnType) {
        Codec codec = lookup(columnType);
        if (codec != null) {
            SpeedyValue sv = codec.decode().apply(instance);
            if (sv instanceof SpeedyDouble sd) return sd;
            throw new RuntimeException("Expected SpeedyDouble from decode, got: " + sv.getClass());
        }
        if (instance instanceof Number n) return new SpeedyDouble(n.doubleValue());
        throw new RuntimeException("Unsupported FLOAT java type: " + instance.getClass());
    }

    private Object convertTextToColumn(SpeedyText speedyText, ColumnType columnType) {
        return encodeOrThrow(columnType, speedyText);
    }

    private Object convertFloatToColumn(SpeedyDouble speedyDouble, ColumnType columnType) {
        Codec codec = lookup(columnType);
        if (codec != null) {
            return codec.encode().apply(speedyDouble);
        }
        return switch (columnType) {
            case FLOAT -> speedyDouble.asDouble().floatValue();
            case DOUBLE -> speedyDouble.asDouble();
            default -> throw new RuntimeException("Unsupported column type: " + columnType);
        };
    }

    private SpeedyValue decodeOrThrow(ColumnType columnType, Object instance) {
        Codec codec = lookup(columnType);
        if (codec == null) {
            throw new RuntimeException("No codec registered for column type: " + columnType);
        }
        return codec.decode().apply(instance);
    }

    private Object encodeOrThrow(ColumnType columnType, SpeedyValue speedyValue) {
        Codec codec = lookup(columnType);
        if (codec == null) {
            throw new RuntimeException("No codec registered for column type: " + columnType);
        }
        return codec.encode().apply(speedyValue);
    }
}
