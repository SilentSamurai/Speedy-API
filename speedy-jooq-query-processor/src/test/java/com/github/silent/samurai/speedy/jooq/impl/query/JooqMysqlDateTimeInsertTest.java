package com.github.silent.samurai.speedy.jooq.impl.query;

import com.github.silent.samurai.speedy.data.StaticFieldMetadata;
import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.jooq.impl.JooqConverters;
import com.github.silent.samurai.speedy.jooq.impl.conversion.Converter;
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

/// Verifies that zoned/instant timestamp values are converted to
/// {@link LocalDateTime} for MySQL/MariaDB before being handed to jOOQ,
/// avoiding the ANSI {@code timestamp with time zone} literal that those
/// dialects reject, and can be decoded back from the {@code LocalDateTime}
/// that MySQL's JDBC driver returns for {@code datetime(6)} columns.
class JooqMysqlDateTimeInsertTest {

    static class DummyEntity {
        private ZonedDateTime createdAt;
    }

    @Test
    void zonedDateTimeInsertOnMysqlDoesNotRenderTimestampWithTimeZone() throws Exception {
        Converter converter = JooqConverters.defaults();
        FieldMetadata fieldMetadata =
                StaticFieldMetadata.createFieldMetadata(DummyEntity.class.getDeclaredField("createdAt"));

        SpeedyValue value = new SpeedyZonedDateTime(ZonedDateTime.parse("2021-01-01T00:00:00+09:00"));
        Object columnValue = converter.toColumnType(value, fieldMetadata);

        assertInstanceOf(OffsetDateTime.class, columnValue,
                "converter should output OffsetDateTime for TIMESTAMP_WITH_ZONE");

        Object dialectValue = JooqUtil.toDialectColumnValue(columnValue, SQLDialect.MYSQL);
        assertInstanceOf(LocalDateTime.class, dialectValue,
                "MySQL must receive LocalDateTime, not OffsetDateTime");

        String sql = DSL.using(SQLDialect.MYSQL)
                .insertInto(table(name("dummy")), field(name("created_at")))
                .values(dialectValue)
                .getSQL(ParamType.INLINED);

        assertFalse(sql.toLowerCase().contains("with time zone"),
                "MySQL INSERT must not render an ANSI 'timestamp with time zone' literal: " + sql);
    }

    @Test
    void zonedDateTimeInsertOnMysqlRoundTripEncodeDecode() throws Exception {
        Converter converter = JooqConverters.defaults();
        FieldMetadata fieldMetadata =
                StaticFieldMetadata.createFieldMetadata(DummyEntity.class.getDeclaredField("createdAt"));

        ZonedDateTime original = ZonedDateTime.parse("2021-01-01T00:00:00+09:00");
        SpeedyValue speedyValue = new SpeedyZonedDateTime(original);

        Object columnValue = converter.toColumnType(speedyValue, fieldMetadata);
        Object dialectValue = JooqUtil.toDialectColumnValue(columnValue, SQLDialect.MYSQL);

        assertInstanceOf(LocalDateTime.class, dialectValue);

        SpeedyValue decoded = converter.toSpeedyValue(dialectValue, fieldMetadata);
        assertInstanceOf(SpeedyZonedDateTime.class, decoded);

        ZonedDateTime roundTripped = decoded.asZonedDateTime();
        assertEquals(original.toInstant(), roundTripped.toInstant(),
                "round-trip through encode+dialect+decode must preserve the instant");
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

        Converter converter = JooqConverters.defaults();
        FieldMetadata fieldMetadata =
                StaticFieldMetadata.createFieldMetadata(DummyEntity.class.getDeclaredField("createdAt"));

        SpeedyValue value = new SpeedyZonedDateTime(ZonedDateTime.parse("2021-01-01T00:00:00+09:00"));
        Object columnValue = converter.toColumnType(value, fieldMetadata);
        Object dialectValue = JooqUtil.toDialectColumnValue(columnValue, SQLDialect.MARIADB);

        assertInstanceOf(LocalDateTime.class, dialectValue);

        SpeedyValue decoded = converter.toSpeedyValue(dialectValue, fieldMetadata);
        assertEquals(value.asZonedDateTime().toInstant(), decoded.asZonedDateTime().toInstant());
    }

    @Test
    void decodeLocalDateTimeFromMySQLReturnsZonedDateTimeAtUTC() throws Exception {
        Converter converter = JooqConverters.defaults();
        FieldMetadata fieldMetadata =
                StaticFieldMetadata.createFieldMetadata(DummyEntity.class.getDeclaredField("createdAt"));

        LocalDateTime fromMySQL = LocalDateTime.parse("2021-01-01T00:00:00");
        SpeedyValue decoded = converter.toSpeedyValue(fromMySQL, fieldMetadata);

        assertInstanceOf(SpeedyZonedDateTime.class, decoded);
        assertEquals(ZoneOffset.UTC, decoded.asZonedDateTime().getZone(),
                "MySQL datetime(6) has no timezone; decoded value should assume UTC");
        assertEquals(fromMySQL, decoded.asZonedDateTime().toLocalDateTime());
    }
}
