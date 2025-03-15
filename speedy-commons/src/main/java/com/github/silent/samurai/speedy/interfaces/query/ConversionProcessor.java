package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.mappings.JavaType2SpeedyValue;
import com.github.silent.samurai.speedy.models.*;
import com.github.silent.samurai.speedy.utils.SpeedyValueFactory;

import java.math.BigInteger;

public interface ConversionProcessor
        <ZonedTimestampType, TimeType, TimestampType, DateType, NumericType,
                DecimalType, RealType, BigIntType, TextType, ClobType, VarcharType, CharType, UuidType> extends Converter {

    default SpeedyValue toSpeedyValue(Object instance, ColumnType columnType) throws SpeedyHttpException {
        return SpeedyValueFactory.toSpeedyValue(columnType.getValueType(), instance);
    }

    default Object toColumnType(SpeedyValue speedyValue, ColumnType columnType) {
        return switch (speedyValue.getValueType()) {
            case BOOL -> switch (columnType) {
                case BOOLEAN -> speedyValue.asBoolean();
                default -> throw new RuntimeException("Unsupported column type: " + columnType);
            };
            case TEXT -> {
                SpeedyText speedyText = (SpeedyText) speedyValue;
                yield switch (columnType) {
                    case UUID -> toUuid(speedyText);
                    case CHAR -> toChar(speedyText);
                    case VARCHAR -> toVarchar(speedyText);
                    case CLOB -> toClob(speedyText);
                    case TEXT -> toText(speedyText);
                    default -> throw new RuntimeException("Unsupported column type: " + columnType);
                };
            }
            case INT -> {
                SpeedyInt speedyInt = (SpeedyInt) speedyValue;
                yield switch (columnType) {
                    case INTEGER, SMALLINT -> speedyInt.asInt();
                    case BIGINT -> toBigInt(speedyInt);
                    default -> throw new RuntimeException("Unsupported column type: " + columnType);
                };
            }
            case FLOAT -> {
                SpeedyDouble speedyDouble = (SpeedyDouble) speedyValue;
                yield switch (columnType) {
                    case REAL -> toReal(speedyDouble);
                    case FLOAT -> speedyDouble.asDouble().floatValue();
                    case DECIMAL -> toDecimal(speedyDouble);
                    case DOUBLE -> speedyDouble.asDouble();
                    case NUMERIC -> toNumeric(speedyDouble);
                    default -> throw new RuntimeException("Unsupported column type: " + columnType);
                };
            }
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

    ZonedTimestampType toZonedTimestamp(SpeedyZonedDateTime speedyZonedDateTime);

    TimeType toTime(SpeedyTime speedyTime);

    TimestampType toTimeStamp(SpeedyDateTime speedyDateTime);

    DateType toDate(SpeedyDate speedyDate);

    NumericType toNumeric(SpeedyDouble speedyDouble);

    DecimalType toDecimal(SpeedyDouble speedyDouble);

    RealType toReal(SpeedyDouble speedyDouble);

    BigIntType toBigInt(SpeedyInt speedyInt);

    TextType toText(SpeedyText speedyText);

    ClobType toClob(SpeedyText speedyText);

    VarcharType toVarchar(SpeedyText speedyText);

    CharType toChar(SpeedyText speedyText);

    UuidType toUuid(SpeedyText speedyText);


}
