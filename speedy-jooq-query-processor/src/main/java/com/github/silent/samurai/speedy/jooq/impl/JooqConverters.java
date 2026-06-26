package com.github.silent.samurai.speedy.jooq.impl;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.jooq.impl.conversion.DbConversionRegistry;
import com.github.silent.samurai.speedy.jooq.impl.conversion.DbConverter;
import com.github.silent.samurai.speedy.models.*;
import com.github.silent.samurai.speedy.utils.Speedy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/// Registers the default JDBC-type codecs into a {@link DbConversionRegistry}.
///
/// Each codec handles exactly **one** Java class — the JDBC driver's native type
/// for the column. No {@code instanceof} branching inside the lambdas; the
/// {@link com.github.silent.samurai.speedy.conversion.codec.Codec#safeDecode(Object)}
/// method verifies the type at runtime via {@link Class#cast}.
public final class JooqConverters {

    private JooqConverters() {
    }

    public static DbConverter defaults() {
        return defaults(null);
    }

    public static DbConverter defaults(DbConversionRegistry parent) {
        DbConversionRegistry r = new DbConversionRegistry(parent);

        r.register(ColumnType.DATE, Date.class,
                sv -> Date.valueOf(((SpeedyDate) sv).asDate()),
                raw -> Speedy.from(raw.toLocalDate()));

        r.register(ColumnType.TIME, Time.class,
                sv -> Time.valueOf(((SpeedyTime) sv).asTime()),
                raw -> Speedy.from(raw.toLocalTime()));

        r.register(ColumnType.TIMESTAMP, Timestamp.class,
                sv -> Timestamp.valueOf(((SpeedyDateTime) sv).asDateTime()),
                raw -> Speedy.from(raw.toLocalDateTime()));

        r.register(ColumnType.TIMESTAMP_WITH_ZONE, OffsetDateTime.class,
                sv -> {
                    SpeedyZonedDateTime szdt = (SpeedyZonedDateTime) sv;
                    return OffsetDateTime.of(szdt.asZonedDateTime().toLocalDateTime(),
                            szdt.asZonedDateTime().getOffset());
                },
                raw -> Speedy.from(raw.toZonedDateTime()));

        r.register(ColumnType.TIMESTAMP_WITH_ZONE, LocalDateTime.class,
                sv -> sv.asZonedDateTime().toLocalDateTime(),
                raw -> Speedy.from(raw.atZone(ZoneOffset.UTC)));

        r.register(ColumnType.NUMERIC, BigDecimal.class,
                sv -> BigDecimal.valueOf(((SpeedyDouble) sv).asDouble()),
                raw -> Speedy.from(raw.doubleValue()));

        r.register(ColumnType.DECIMAL, BigDecimal.class,
                sv -> BigDecimal.valueOf(((SpeedyDouble) sv).asDouble()),
                raw -> Speedy.from(raw.doubleValue()));

        r.register(ColumnType.REAL, Double.class,
                sv -> ((SpeedyDouble) sv).asDouble(),
                raw -> Speedy.from(raw));

        r.register(ColumnType.BIGINT, BigInteger.class,
                sv -> BigInteger.valueOf(((SpeedyInt) sv).asLong()),
                raw -> Speedy.from(raw.longValue()));

        r.register(ColumnType.TEXT, String.class,
                sv -> ((SpeedyText) sv).asText(),
                raw -> Speedy.from(raw));

        r.register(ColumnType.CLOB, String.class,
                sv -> ((SpeedyText) sv).asText(),
                raw -> Speedy.from(raw));

        r.register(ColumnType.VARCHAR, String.class,
                sv -> ((SpeedyText) sv).asText(),
                raw -> Speedy.from(raw));

        r.register(ColumnType.CHAR, String.class,
                sv -> ((SpeedyText) sv).asText(),
                raw -> Speedy.from(raw));

        r.register(ColumnType.UUID, UUID.class,
                sv -> UUID.fromString(((SpeedyText) sv).asText()),
                raw -> Speedy.from(raw.toString()));

        return new DbConverter(r);
    }
}
