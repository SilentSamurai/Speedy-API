package com.github.silent.samurai.speedy.jooq.impl.query;

import com.github.silent.samurai.speedy.data.StaticFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.jooq.impl.JooqConverters;
import com.github.silent.samurai.speedy.jooq.impl.conversion.Converter;
import com.github.silent.samurai.speedy.models.SpeedyZonedDateTime;
import org.jooq.SQLDialect;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;
import static org.junit.jupiter.api.Assertions.*;

/// Verifies that zoned/instant timestamp values are converted to
/// {@link LocalDateTime} for MySQL/MariaDB before being handed to jOOQ,
/// avoiding the ANSI {@code timestamp with time zone} literal that those
/// dialects reject.
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
}
