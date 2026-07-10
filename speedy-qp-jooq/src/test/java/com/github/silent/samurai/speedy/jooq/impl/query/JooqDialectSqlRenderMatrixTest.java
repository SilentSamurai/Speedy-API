package com.github.silent.samurai.speedy.jooq.impl.query;

import com.github.silent.samurai.speedy.data.StaticFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.jooq.impl.conversion.TypeConverter;
import com.github.silent.samurai.speedy.utils.Speedy;
import org.jooq.SQLDialect;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Render-lint over the {@code (ColumnType × SQLDialect)} matrix. The unit gate runs against H2,
/// which happily accepts ANSI syntax (e.g. {@code timestamp with time zone}) that MySQL/MariaDB
/// reject — so a whole class of "we generated SQL this dialect can't parse" bugs is invisible in
/// the fast inner loop and only surfaces against a real database.
///
/// This test generalises {@link JooqMysqlDateTimeInsertTest}: for every representative column type
/// and every dialect, it drives the *real* dialect-aware converter, renders an INSERT, and asserts
/// the SQL contains no dialect-illegal construct. It needs no database connection — jOOQ renders SQL
/// for any dialect offline — so it catches the class cheaply rather than one combo at a time.
class JooqDialectSqlRenderMatrixTest {

    /// One field per representative column type reachable through {@link StaticFieldMetadata}'s
    /// Java-type mapping. Includes the {@code TIMESTAMP_WITH_ZONE} case that motivated this test.
    static class MatrixEntity {
        Boolean booleanValue;
        Integer intValue;
        Double doubleValue;
        String textValue;
        UUID uuidValue;
        LocalDate localDate;
        LocalTime localTime;
        LocalDateTime localDateTime;
        ZonedDateTime zonedDateTime;
    }

    private static final List<SQLDialect> DIALECTS =
            List.of(SQLDialect.H2, SQLDialect.MYSQL, SQLDialect.MARIADB, SQLDialect.POSTGRES);

    @Test
    void everyColumnTypeRendersDialectLegalInsertSql() {
        List<String> violations = new ArrayList<>();

        for (Field reflectField : MatrixEntity.class.getDeclaredFields()) {
            FieldMetadata fm = StaticFieldMetadata.createFieldMetadata(reflectField);
            SpeedyValue value = sampleValue(fm);

            for (SQLDialect dialect : DIALECTS) {
                String sql;
                try {
                    Object columnValue = TypeConverter.defaults(dialect).toColumnType(value, fm);
                    sql = DSL.using(dialect)
                            .insertInto(table(name("matrix")), field(name(fm.getDbColumnName())))
                            .values(columnValue)
                            .getSQL(ParamType.INLINED);
                } catch (Exception e) {
                    violations.add(dialect + " / " + fm.getColumnType() + ": rendering threw " + e);
                    continue;
                }

                // MySQL/MariaDB have no TIMESTAMP WITH TIME ZONE; jOOQ renders OffsetDateTime as the
                // ANSI literal those dialects reject. The dialect-aware converter must have avoided it.
                if ((dialect == SQLDialect.MYSQL || dialect == SQLDialect.MARIADB)
                        && sql.toLowerCase().contains("with time zone")) {
                    violations.add(dialect + " / " + fm.getColumnType()
                            + ": rendered ANSI 'timestamp with time zone' literal: " + sql);
                }
            }
        }

        assertTrue(violations.isEmpty(),
                () -> "Dialect-illegal SQL rendered:\n  " + String.join("\n  ", violations));
    }

    /// A representative SpeedyValue for the field's column type. Keyed on ColumnType (not Java type)
    /// so it stays correct if {@link StaticFieldMetadata}'s mapping changes.
    private static SpeedyValue sampleValue(FieldMetadata fm) {
        return switch (fm.getColumnType()) {
            case BOOLEAN -> Speedy.from(Boolean.TRUE);
            case INTEGER, SMALLINT, BIGINT -> Speedy.from(123L);
            case FLOAT, DOUBLE, REAL, NUMERIC, DECIMAL -> Speedy.from(1.5);
            case TEXT, VARCHAR, CHAR, CLOB -> Speedy.from("sample");
            case UUID -> Speedy.from(UUID.fromString("11111111-1111-1111-1111-111111111111").toString());
            case DATE -> Speedy.from(LocalDate.of(2021, 1, 1));
            case TIME -> Speedy.from(LocalTime.of(12, 0));
            case TIMESTAMP -> Speedy.from(LocalDateTime.parse("2021-01-01T00:00:00"));
            case TIMESTAMP_WITH_ZONE -> Speedy.from(ZonedDateTime.parse("2021-01-01T00:00:00+09:00"));
            default -> throw new IllegalArgumentException("no sample value for " + fm.getColumnType());
        };
    }
}
