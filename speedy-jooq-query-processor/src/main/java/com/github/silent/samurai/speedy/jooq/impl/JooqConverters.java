package com.github.silent.samurai.speedy.jooq.impl;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.mappings.DbConversionRegistry;
import com.github.silent.samurai.speedy.models.*;
import com.github.silent.samurai.speedy.utils.Speedy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class JooqConverters {

    private JooqConverters() {
    }

    public static DbConversionRegistry defaults() {
        return defaults(null);
    }

    public static DbConversionRegistry defaults(DbConversionRegistry parent) {
        DbConversionRegistry r = new DbConversionRegistry(parent);

        r.register(ColumnType.DATE,
                sv -> Date.valueOf(((SpeedyDate) sv).asDate()),
                raw -> Speedy.from(((Date) raw).toLocalDate()));

        r.register(ColumnType.TIME,
                sv -> Time.valueOf(((SpeedyTime) sv).asTime()),
                raw -> Speedy.from(((Time) raw).toLocalTime()));

        r.register(ColumnType.TIMESTAMP,
                sv -> Timestamp.valueOf(((SpeedyDateTime) sv).asDateTime()),
                raw -> Speedy.from(((Timestamp) raw).toLocalDateTime()));

        r.register(ColumnType.TIMESTAMP_WITH_ZONE,
                sv -> {
                    SpeedyZonedDateTime szdt = (SpeedyZonedDateTime) sv;
                    return OffsetDateTime.of(szdt.asZonedDateTime().toLocalDateTime(),
                            szdt.asZonedDateTime().getOffset());
                },
                raw -> Speedy.from(((OffsetDateTime) raw).toZonedDateTime()));

        r.register(ColumnType.NUMERIC,
                sv -> BigDecimal.valueOf(((SpeedyDouble) sv).asDouble()),
                raw -> Speedy.from(((BigDecimal) raw).doubleValue()));

        r.register(ColumnType.DECIMAL,
                sv -> BigDecimal.valueOf(((SpeedyDouble) sv).asDouble()),
                raw -> Speedy.from(((BigDecimal) raw).doubleValue()));

        r.register(ColumnType.REAL,
                sv -> ((SpeedyDouble) sv).asDouble(),
                raw -> Speedy.from((Double) raw));

        r.register(ColumnType.BIGINT,
                sv -> BigInteger.valueOf(((SpeedyInt) sv).asLong()),
                raw -> Speedy.from(((BigInteger) raw).longValue()));

        r.register(ColumnType.TEXT,
                sv -> ((SpeedyText) sv).asText(),
                raw -> Speedy.from((String) raw));

        r.register(ColumnType.CLOB,
                sv -> ((SpeedyText) sv).asText(),
                raw -> Speedy.from((String) raw));

        r.register(ColumnType.VARCHAR,
                sv -> ((SpeedyText) sv).asText(),
                raw -> Speedy.from((String) raw));

        r.register(ColumnType.CHAR,
                sv -> ((SpeedyText) sv).asText(),
                raw -> Speedy.from((String) raw));

        r.register(ColumnType.UUID,
                sv -> UUID.fromString(((SpeedyText) sv).asText()),
                raw -> Speedy.from(((UUID) raw).toString()));

        return r;
    }
}
