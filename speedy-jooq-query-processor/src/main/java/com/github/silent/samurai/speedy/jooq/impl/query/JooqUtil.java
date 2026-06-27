package com.github.silent.samurai.speedy.jooq.impl.query;

import com.github.silent.samurai.speedy.dialects.SpeedyDialect;
import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.jooq.impl.Dialects;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;


import java.util.Objects;
import java.util.Optional;

/// Static utilities for jOOQ table/column references, SQL data-type mapping,
/// SpeedyDialect conversion, and identifier name transformation.
public class JooqUtil {

    static String transformSqlNames = "TO_UPPERCASE";

    public static void transformSqlNames(String setting) {
        transformSqlNames = setting;
    }

    public static DataType<?> getSQLDataType(String fieldReference, ColumnType columnType, SQLDialect dialect) {
        return Dialects.forJooq(dialect).sqlDataType(columnType);
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
        DataType<?> sqlDataType = JooqUtil.getSQLDataType(fieldMetadata.getDbColumnName(), columnType, dialect);
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
            case H2, H2_1_4_197, H2_1_4_200, H2_2_0_202, H2_2_1_214 -> SQLDialect.H2;
            case MYSQL, MYSQL_5_6, MYSQL_5_7, MYSQL_8_0, MYSQL_8_0_19, MYSQL_8_0_20, MYSQL_8_0_31,
                 AURORA_MYSQL, MEMSQL, MEMSQL_6, MEMSQL_7, MEMSQL_8 -> SQLDialect.MYSQL;
            case POSTGRES, POSTGRES_10, POSTGRES_11, POSTGRES_12, POSTGRES_13,
                 POSTGRES_14, POSTGRES_15, POSTGRES_9_3, POSTGRES_9_4, POSTGRES_9_5,
                 AURORA_POSTGRES, COCKROACHDB, COCKROACHDB_20, COCKROACHDB_21, COCKROACHDB_22,
                 POSTGRESPLUS, REDSHIFT, YUGABYTEDB, YUGABYTEDB_2_9 -> SQLDialect.POSTGRES;
            case SQLITE, SQLITE_3_25, SQLITE_3_28, SQLITE_3_30, SQLITE_3_38, SQLITE_3_39 -> SQLDialect.SQLITE;
            case MARIADB, MARIADB_10_0, MARIADB_10_1, MARIADB_10_2, MARIADB_10_3,
                 MARIADB_10_4, MARIADB_10_5, MARIADB_10_6, MARIADB_10_7 -> SQLDialect.MARIADB;
            case DERBY -> SQLDialect.DERBY;
            case FIREBIRD, FIREBIRD_2_5, FIREBIRD_3_0, FIREBIRD_4_0 -> SQLDialect.FIREBIRD;
            case HSQLDB -> SQLDialect.HSQLDB;
            case DUCKDB -> SQLDialect.DUCKDB;
            case TRINO -> SQLDialect.TRINO;
            default -> SQLDialect.DEFAULT;
        };
    }

    public static String transformIdentifier(String identifier, SQLDialect sqlDialect) {
        return Dialects.forJooq(sqlDialect).transformIdentifier(identifier);
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
    UUID
     */
}
