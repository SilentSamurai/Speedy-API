package com.github.silent.samurai.speedy.mappings;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.query.Converter;
import com.github.silent.samurai.speedy.models.*;

public abstract class ConversionFactory
        <ZonedTimestampType, TimeType, TimestampType, DateType, NumericType,
                DecimalType, RealType, BigIntType, TextType, ClobType, VarcharType, CharType, UuidType> implements Converter {

    public SpeedyValue toSpeedyValue(Object instance, FieldMetadata fieldMetadata) throws SpeedyHttpException {
        if (instance == null) {
            return SpeedyNull.SPEEDY_NULL;
        }

        ColumnType columnType = fieldMetadata.getColumnType();
        ValueType valueType = fieldMetadata.getValueType();

        // First, prefer explicit ValueType mapping where possible (covers enums, etc.)
        return switch (valueType) {
            case BOOL:
                if (instance instanceof Boolean b) {
                    yield new SpeedyBoolean(b);
                }
                throw new RuntimeException("Unsupported BOOL java type: " + instance.getClass());
            case TEXT:
                yield convertTextFromColumn(instance, columnType);
            case ENUM:
                try {
                    yield new SpeedyEnum(String.valueOf(instance), fieldMetadata);
                } catch (SpeedyHttpException e) {
                    throw new RuntimeException(e);
                }
            case ENUM_ORD:
                try {
                    if (instance instanceof Number n) {
                        yield new SpeedyEnum(n.longValue(), fieldMetadata);
                    }
                    yield new SpeedyEnum(Long.parseLong(String.valueOf(instance)), fieldMetadata);
                } catch (SpeedyHttpException e) {
                    throw new RuntimeException(e);
                }
            case INT:
                // BIGINT can map to BigInteger in some drivers, but many JDBC drivers return Long.
                if (columnType == ColumnType.BIGINT && instance instanceof java.math.BigInteger) {
                    // Safe cast because of the instanceof check
                    yield fromBigInt((BigIntType) instance);
                }
                if (instance instanceof Number n) {
                    // Handles Long, Integer, Short and any other numeric representation
                    yield new SpeedyInt(n.longValue());
                }
                throw new RuntimeException("Unsupported INT java type: " + instance.getClass());
            case FLOAT:
                yield convertFloatFromColumn(instance, columnType);
            case DATE:
                yield fromDate((DateType) instance);
            case TIME:
                yield fromTime((TimeType) instance);
            case DATE_TIME:
                yield fromTimeStamp((TimestampType) instance);
            case ZONED_DATE_TIME:
                yield fromZonedTimestamp((ZonedTimestampType) instance);
            case OBJECT:
            case COLLECTION:
                throw new RuntimeException("Cannot convert container types from column value");
            case NULL:
                yield SpeedyNull.SPEEDY_NULL;
        };
    }

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
                    yield switch (columnType) {
                        case CHAR -> toChar(st);
                        case VARCHAR -> toVarchar(st);
                        case TEXT -> toText(st);
                        case CLOB -> toClob(st);
                        default ->
                                throw new RuntimeException("Enum stored as STRING cannot be written to column type: " + columnType);
                    };
                }
                case ORDINAL -> {
                    DynamicEnum.Variant v = fieldMetadata.getDynamicEnum()
                            .fromName(speedyValue.asEnum())
                            .orElseThrow(() -> new RuntimeException("Enum value '" + speedyValue.asEnum() + "' is not valid."));
                    SpeedyInt speedyInt = new SpeedyInt((long) v.code());
                    yield switch (columnType) {
                        case INTEGER, SMALLINT -> speedyInt.asInt();
                        case BIGINT -> toBigInt(speedyInt);
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
                    yield switch (columnType) {
                        case CHAR -> toChar(st);
                        case VARCHAR -> toVarchar(st);
                        case TEXT -> toText(st);
                        case CLOB -> toClob(st);
                        default ->
                                throw new RuntimeException("Enum stored as ORDINAL cannot be written to column type: " + columnType);
                    };
                }
                case ORDINAL -> {
                    SpeedyInt speedyInt = new SpeedyInt(speedyValue.asEnumOrd());
                    yield switch (columnType) {
                        case INTEGER, SMALLINT -> speedyInt.asInt();
                        case BIGINT -> toBigInt(speedyInt);
                        default ->
                                throw new RuntimeException("Enum stored as ORDINAL cannot be written to column type: " + columnType);
                    };
                }
            };
            case INT -> {
                SpeedyInt speedyInt = (SpeedyInt) speedyValue;
                yield switch (columnType) {
                    case INTEGER, SMALLINT -> speedyInt.asInt();
                    case BIGINT -> toBigInt(speedyInt);
                    default -> throw new RuntimeException("Unsupported column type: " + columnType);
                };
            }
            case FLOAT -> convertFloatToColumn((SpeedyDouble) speedyValue, columnType);
            case DATE -> {
                SpeedyDate speedyDate = (SpeedyDate) speedyValue;
                yield switch (columnType) {
                    case DATE -> toDate(speedyDate);
                    default -> throw new RuntimeException("Unsupported column type: " + columnType);
                };
            }
            case TIME -> {
                SpeedyTime speedyTime = (SpeedyTime) speedyValue;
                yield switch (columnType) {
                    case TIME -> toTime(speedyTime);
                    default -> throw new RuntimeException("Unsupported column type: " + columnType);
                };
            }
            case DATE_TIME -> {
                SpeedyDateTime speedyDateTime = (SpeedyDateTime) speedyValue;
                yield switch (columnType) {
                    case TIMESTAMP -> toTimeStamp(speedyDateTime);
                    default -> throw new RuntimeException("Unsupported column type: " + columnType);
                };
            }
            case ZONED_DATE_TIME -> {
                SpeedyZonedDateTime speedyZonedDateTime = (SpeedyZonedDateTime) speedyValue;
                yield switch (columnType) {
                    case TIMESTAMP_WITH_ZONE -> toZonedTimestamp(speedyZonedDateTime);
                    default -> throw new RuntimeException("Unsupported column type: " + columnType);
                };
            }
            case OBJECT, COLLECTION -> {
                throw new RuntimeException("Cannot convert from " + speedyValue + " to " + columnType);
            }
            case NULL -> null;
        };
    }

    // ---------------------------------------------------------------------
    // Helper methods to centralize TEXT and FLOAT conversions
    // ---------------------------------------------------------------------

    private SpeedyValue convertTextFromColumn(Object instance, ColumnType columnType) {
        return switch (columnType) {
            case UUID -> fromUuid((UuidType) instance);
            case CHAR -> fromChar((CharType) instance);
            case VARCHAR -> fromVarchar((VarcharType) instance);
            case CLOB -> fromClob((ClobType) instance);
            case TEXT -> fromText((TextType) instance);
            default -> new SpeedyText(String.valueOf(instance));
        };
    }

    private SpeedyDouble convertFloatFromColumn(Object instance, ColumnType columnType) {
        return switch (columnType) {
            case NUMERIC -> fromNumeric((NumericType) instance);
            case DECIMAL -> fromDecimal((DecimalType) instance);
            case REAL -> fromReal((RealType) instance);
            case DOUBLE -> {
                if (instance instanceof Number n) yield new SpeedyDouble(n.doubleValue());
                throw new RuntimeException("Unsupported DOUBLE java type: " + instance.getClass());
            }
            case FLOAT -> {
                if (instance instanceof Number n) yield new SpeedyDouble(n.doubleValue());
                throw new RuntimeException("Unsupported FLOAT java type: " + instance.getClass());
            }
            default -> {
                if (instance instanceof Number n) yield new SpeedyDouble(n.doubleValue());
                throw new RuntimeException("Unsupported FLOAT mapping for column type: " + columnType);
            }
        };
    }

    private Object convertTextToColumn(SpeedyText speedyText, ColumnType columnType) {
        return switch (columnType) {
            case UUID -> toUuid(speedyText);
            case CHAR -> toChar(speedyText);
            case VARCHAR -> toVarchar(speedyText);
            case CLOB -> toClob(speedyText);
            case TEXT -> toText(speedyText);
            default -> throw new RuntimeException("Unsupported column type: " + columnType);
        };
    }

    private Object convertFloatToColumn(SpeedyDouble speedyDouble, ColumnType columnType) {
        return switch (columnType) {
            case REAL -> toReal(speedyDouble);
            case FLOAT -> speedyDouble.asDouble().floatValue();
            case DECIMAL -> toDecimal(speedyDouble);
            case DOUBLE -> speedyDouble.asDouble();
            case NUMERIC -> toNumeric(speedyDouble);
            default -> throw new RuntimeException("Unsupported column type: " + columnType);
        };
    }

    public abstract ZonedTimestampType toZonedTimestamp(SpeedyZonedDateTime speedyZonedDateTime);

    public abstract SpeedyZonedDateTime fromZonedTimestamp(ZonedTimestampType zonedTimestamp);

    public abstract TimeType toTime(SpeedyTime speedyTime);

    public abstract SpeedyTime fromTime(TimeType time);

    public abstract TimestampType toTimeStamp(SpeedyDateTime speedyDateTime);

    public abstract SpeedyDateTime fromTimeStamp(TimestampType timestamp);

    public abstract DateType toDate(SpeedyDate speedyDate);

    public abstract SpeedyDate fromDate(DateType date);

    public abstract NumericType toNumeric(SpeedyDouble speedyDouble);

    public abstract SpeedyDouble fromNumeric(NumericType numeric);

    public abstract DecimalType toDecimal(SpeedyDouble speedyDouble);

    public abstract SpeedyDouble fromDecimal(DecimalType decimal);

    public abstract RealType toReal(SpeedyDouble speedyDouble);

    public abstract SpeedyDouble fromReal(RealType real);

    public abstract BigIntType toBigInt(SpeedyInt speedyInt);

    public abstract SpeedyInt fromBigInt(BigIntType bigInt);

    public abstract TextType toText(SpeedyText speedyText);

    public abstract SpeedyText fromText(TextType text);

    public abstract ClobType toClob(SpeedyText speedyText);

    public abstract SpeedyText fromClob(ClobType clob);

    public abstract VarcharType toVarchar(SpeedyText speedyText);

    public abstract SpeedyText fromVarchar(VarcharType varchar);

    public abstract CharType toChar(SpeedyText speedyText);

    public abstract SpeedyText fromChar(CharType char_v);

    public abstract UuidType toUuid(SpeedyText speedyText);

    public abstract SpeedyText fromUuid(UuidType uuid);
}
