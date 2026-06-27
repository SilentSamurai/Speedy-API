package com.github.silent.samurai.speedy.jooq.impl.query;

import com.github.silent.samurai.speedy.data.StaticFieldMetadata;
import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.jooq.impl.conversion.TypeConverter;
import com.github.silent.samurai.speedy.models.SpeedyZonedDateTime;
import org.jooq.SQLDialect;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;
import static org.junit.jupiter.api.Assertions.*;

/// Verifies that zoned/instant timestamp values are encoded to {@link LocalDateTime} for
/// MySQL/MariaDB *by the dialect-aware converter itself* (no post-hoc fixup), avoiding the ANSI
/// {@code timestamp with time zone} literal those dialects reject, and can be decoded back from the
/// {@code LocalDateTime} that MySQL's JDBC driver returns for {@code datetime(6)} columns.
class JooqMysqlDateTimeInsertTest {

    static class DummyEntity {
        private ZonedDateTime createdAt;
    }

    private static FieldMetadata createdAt() throws Exception {
        return StaticFieldMetadata.createFieldMetadata(DummyEntity.class.getDeclaredField("createdAt"));
    }

    @Test
    void converterIsDialectAwareForZonedTimestamp() throws Exception {
        FieldMetadata fieldMetadata = createdAt();
        SpeedyValue value = new SpeedyZonedDateTime(ZonedDateTime.parse("2021-01-01T00:00:00+09:00"));

        // MySQL/MariaDB cannot store an offset, so the converter must hand jOOQ a LocalDateTime.
        assertInstanceOf(LocalDateTime.class, TypeConverter.defaults(SQLDialect.MYSQL).toColumnType(value, fieldMetadata),
                "MySQL converter must produce LocalDateTime");
        // Dialects that support the type keep the offset.
        assertInstanceOf(OffsetDateTime.class, TypeConverter.defaults(SQLDialect.POSTGRES).toColumnType(value, fieldMetadata),
                "Postgres converter must keep OffsetDateTime");
    }

    @Test
    void zonedDateTimeInsertOnMysqlDoesNotRenderTimestampWithTimeZone() throws Exception {
        FieldMetadata fieldMetadata = createdAt();
        SpeedyValue value = new SpeedyZonedDateTime(ZonedDateTime.parse("2021-01-01T00:00:00+09:00"));

        Object dialectValue = TypeConverter.defaults(SQLDialect.MYSQL).toColumnType(value, fieldMetadata);
        assertInstanceOf(LocalDateTime.class, dialectValue, "MySQL must receive LocalDateTime, not OffsetDateTime");

        String sql = DSL.using(SQLDialect.MYSQL)
                .insertInto(table(name("dummy")), field(name("created_at")))
                .values(dialectValue)
                .getSQL(ParamType.INLINED);

        assertFalse(sql.toLowerCase().contains("with time zone"),
                "MySQL INSERT must not render an ANSI 'timestamp with time zone' literal: " + sql);
    }

    @Test
    void zonedDateTimeInsertOnMysqlRoundTripEncodeDecode() throws Exception {
        TypeConverter converter = TypeConverter.defaults(SQLDialect.MYSQL);
        FieldMetadata fieldMetadata = createdAt();

        ZonedDateTime original = ZonedDateTime.parse("2021-01-01T00:00:00+09:00");
        Object dialectValue = converter.toColumnType(new SpeedyZonedDateTime(original), fieldMetadata);
        assertInstanceOf(LocalDateTime.class, dialectValue);

        SpeedyValue decoded = converter.toSpeedyValue(dialectValue, fieldMetadata);
        assertInstanceOf(SpeedyZonedDateTime.class, decoded);
        assertEquals(original.toInstant(), decoded.asZonedDateTime().toInstant(),
                "round-trip through encode+decode must preserve the instant");
    }

    @Test
    void mysqlDataTypeMappingUsesLocalDateTime() {
        var dataType = JooqUtil.getSQLDataType("created_at", ColumnType.TIMESTAMP, SQLDialect.MYSQL);
        assertEquals(SQLDataType.TIMESTAMP, dataType);

        var withZone = JooqUtil.getSQLDataType("created_at", ColumnType.TIMESTAMP_WITH_ZONE, SQLDialect.MYSQL);
        assertEquals(SQLDataType.LOCALDATETIME, withZone,
                "MySQL must map TIMESTAMP_WITH_ZONE to LOCALDATETIME");

        var h2 = JooqUtil.getSQLDataType("created_at", ColumnType.TIMESTAMP_WITH_ZONE, SQLDialect.H2);
        assertEquals(SQLDataType.TIMESTAMPWITHTIMEZONE, h2,
                "H2 must preserve TIMESTAMPWITHTIMEZONE");
    }

    @Test
    void mariaDBBehavesLikeMySQL() throws Exception {
        var withZone = JooqUtil.getSQLDataType("created_at", ColumnType.TIMESTAMP_WITH_ZONE, SQLDialect.MARIADB);
        assertEquals(SQLDataType.LOCALDATETIME, withZone);

        TypeConverter converter = TypeConverter.defaults(SQLDialect.MARIADB);
        FieldMetadata fieldMetadata = createdAt();

        SpeedyValue value = new SpeedyZonedDateTime(ZonedDateTime.parse("2021-01-01T00:00:00+09:00"));
        Object dialectValue = converter.toColumnType(value, fieldMetadata);
        assertInstanceOf(LocalDateTime.class, dialectValue);

        SpeedyValue decoded = converter.toSpeedyValue(dialectValue, fieldMetadata);
        assertEquals(value.asZonedDateTime().toInstant(), decoded.asZonedDateTime().toInstant());
    }

    @Test
    void decodeLocalDateTimeFromMySQLReturnsZonedDateTimeAtUTC() throws Exception {
        TypeConverter converter = TypeConverter.defaults(SQLDialect.MYSQL);
        FieldMetadata fieldMetadata = createdAt();

        LocalDateTime fromMySQL = LocalDateTime.parse("2021-01-01T00:00:00");
        SpeedyValue decoded = converter.toSpeedyValue(fromMySQL, fieldMetadata);

        assertInstanceOf(SpeedyZonedDateTime.class, decoded);
        assertEquals(ZoneOffset.UTC, decoded.asZonedDateTime().getZone(),
                "MySQL datetime(6) has no timezone; decoded value should assume UTC");
        assertEquals(fromMySQL, decoded.asZonedDateTime().toLocalDateTime());
    }
}
