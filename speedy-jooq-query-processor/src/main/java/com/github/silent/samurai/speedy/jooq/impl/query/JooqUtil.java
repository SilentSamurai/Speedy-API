package com.github.silent.samurai.speedy.jooq.impl.query;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;


import java.util.Objects;
import java.util.Optional;

/// Static utilities for jOOQ table/column references, SQL data-type mapping,
/// SpeedyDialect conversion, and identifier name transformation.
public class JooqUtil {

    static String transformSqlNames = "TO_UPPERCASE";

    public static void transformSqlNames(String setting) {
        transformSqlNames = setting;
    }

    public static DataType<?> getSQLDataType(String fieldReference, ColumnType columnType) {
        return switch (columnType) {
            case INTEGER:
                yield SQLDataType.INTEGER;
            case SMALLINT:
                yield SQLDataType.SMALLINT;
            case BIGINT:
                yield SQLDataType.BIGINT;
            case DECIMAL:
                yield SQLDataType.DECIMAL;
            case NUMERIC:
                yield SQLDataType.NUMERIC;
            case FLOAT:
                yield SQLDataType.FLOAT;
            case REAL:
                yield SQLDataType.REAL;
            case DOUBLE:
                yield SQLDataType.DOUBLE;
            case CHAR:
                yield SQLDataType.CHAR;
            case VARCHAR, TEXT:
                yield SQLDataType.VARCHAR;
            case DATE:
                yield SQLDataType.DATE;
            case TIME:
                yield SQLDataType.TIME;
            case TIMESTAMP:
                yield SQLDataType.TIMESTAMP;
            case TIMESTAMP_WITH_ZONE:
                yield SQLDataType.TIMESTAMPWITHTIMEZONE;
            case BOOLEAN:
                yield SQLDataType.BOOLEAN;
            case BLOB:
                yield SQLDataType.BLOB;
            case CLOB:
                yield SQLDataType.CLOB;
            case UUID:
                yield SQLDataType.UUID;
            default:
                throw new RuntimeException(String.format("DataType not supported: {} for field {}", columnType, fieldReference));
        };
    }

    public static Table<Record> getTable(EntityMetadata entityMetadata, SQLDialect dialect) {
        String name = entityMetadata.getDbTableName();
        Objects.requireNonNull(name);
        name = transformIdentifier(name, dialect);
        return DSL.table(DSL.name(name));
    }

    public static <T> Field<T> getColumn(FieldMetadata fieldMetadata, SQLDialect dialect) {
        EntityMetadata entityMetadata = fieldMetadata.getEntityMetadata();
        Table<?> table = JooqUtil.getTable(entityMetadata, dialect);
        return getTypedField(fieldMetadata, table, dialect);
    }

    public static <T> Field<T> getColumnWithTableAlias(String tableAlias, FieldMetadata fieldMetadata, SQLDialect dialect) {
        Table<?> table = DSL.table(DSL.name(tableAlias));
        return getTypedField(fieldMetadata, table, dialect);
    }

    /// The metadata describing a field's stored value type. For an association field the stored value is
    /// the foreign key, typed by the associated (target primary-key) field; for a plain field it is the
    /// field itself. Centralises the FK special-case shared by column typing and value conversion.
    static FieldMetadata conversionField(FieldMetadata fieldMetadata) {
        return fieldMetadata.isAssociation() ? fieldMetadata.getAssociatedFieldMetadata() : fieldMetadata;
    }

    private static <T> Field<T> getTypedField(FieldMetadata fieldMetadata, Table<?> table, SQLDialect dialect) {
        ColumnType columnType = conversionField(fieldMetadata).getColumnType();
        DataType<?> sqlDataType = JooqUtil.getSQLDataType(fieldMetadata.getDbColumnName(), columnType);
        Objects.requireNonNull(fieldMetadata.getDbColumnName());
        Name columnName = DSL.name(
                table.getName(),
                transformIdentifier(fieldMetadata.getDbColumnName(), dialect)
        );
        return (Field<T>) DSL.field(columnName, sqlDataType);
    }

    public static <T> Optional<T> getValueFromRecord(Record record, FieldMetadata fieldMetadata, SQLDialect dialect) {
        // should never happen
        if (fieldMetadata.getDbColumnName() == null) {
            return Optional.empty();
        }
        Field<T> column = JooqUtil.getColumn(fieldMetadata, dialect);
        try {
            T value = record.get(column, column.getType());
            return Optional.ofNullable(value);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static SQLDialect toJooqDialect(SpeedyDialect dialect) {
        return switch (dialect) {
            case H2 -> SQLDialect.H2;
            case MYSQL -> SQLDialect.MYSQL;
            case POSTGRES -> SQLDialect.POSTGRES;
            case SQLITE -> SQLDialect.SQLITE;
            case MARIADB -> SQLDialect.MARIADB;
            case DERBY -> SQLDialect.DERBY;
            case FIREBIRD -> SQLDialect.FIREBIRD;
            case HSQLDB -> SQLDialect.HSQLDB;
            case DUCKDB -> SQLDialect.DUCKDB;
            case TRINO -> SQLDialect.TRINO;
            case YUGABYTEDB -> SQLDialect.YUGABYTEDB;
            default -> SQLDialect.DEFAULT;
        };
    }

    public static String transformIdentifier(String identifier, SQLDialect sqlDialect) {
        return switch (sqlDialect) {
            case H2 -> identifier.toUpperCase();
            default -> camelToSnake(identifier);
        };
    }

    /// Converts a camelCase identifier to snake_case, inserting a separator at lower/digit-to-upper
    /// boundaries ({@code firstName -> first_name}, {@code address1Line -> address1_line}) and at
    /// acronym-to-word boundaries ({@code userIDCard -> user_id_card}), then lower-casing. A faithful
    /// replacement for the removed Spring {@code ParsingUtils.reconcatenateCamelCase}; the previous
    /// single-rule {@code ([a-z])([A-Z])} regex silently dropped the acronym and digit boundaries.
    private static String camelToSnake(String identifier) {
        return identifier
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .toLowerCase();
    }

    /* Jooq to java types
    BOOLEAN	Boolean
    TINYINT	Byte
    SMALLINT	Short
    INTEGER	Integer
    BIGINT	Long
    REAL	Float
    FLOAT	Double
    DOUBLE	Double
    DECIMAL / NUMERIC	BigDecimal
    CHAR / VARCHAR / TEXT	String
    DATE	LocalDate
    TIME	LocalTime
    TIMESTAMP	LocalDateTime
    TIMESTAMP WITH TIME ZONE	OffsetDateTime
    BLOB	byte[]
    UUID	UUID
     */

//    public static Object toJooqType(SpeedyValue value, ColumnType columnType) {
//        if (value == SpeedyNull.SPEEDY_NULL) {
//            return null;
//        }
//        return switch (columnType) {
//            case INTEGER -> value.asLong();
//            case BIGINT -> BigInteger.valueOf(value.asLong());
//            case SMALLINT -> value.asInt();
//            case DECIMAL, NUMERIC -> BigDecimal.valueOf(value.asDouble());
//            case FLOAT, REAL, DOUBLE -> value.asDouble();
//            case CHAR, VARCHAR, TEXT, CLOB -> value.asText();
//            case DATE -> Date.valueOf(value.asDate());
//            case TIME -> java.sql.Time.valueOf(value.asTime());
//            case TIMESTAMP -> java.sql.Timestamp.valueOf(value.asDateTime());
//            case TIMESTAMP_WITH_ZONE ->
//                    OffsetDateTime.of(value.asZonedDateTime().toLocalDateTime(), value.asZonedDateTime().getOffset());
//            case BOOLEAN -> value.asBoolean();
//            case BLOB -> value.asText().getBytes(StandardCharsets.UTF_8);
//            case UUID -> UUID.fromString(value.asText());
//        };
//    }
}
