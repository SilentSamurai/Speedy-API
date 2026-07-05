package com.github.silent.samurai.speedy.jooq.impl.dialect;

import com.github.silent.samurai.speedy.conversion.codec.Codec;
import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.jooq.impl.Dialects;
import com.github.silent.samurai.speedy.jooq.impl.conversion.CodecRegistry;
import com.github.silent.samurai.speedy.models.*;
import com.github.silent.samurai.speedy.utils.Speedy;
import org.jooq.DataType;
import org.jooq.impl.SQLDataType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/// Base dialect strategy *and* the default dialect itself. Holds every per-dialect decision the jOOQ
/// backend makes, with the ANSI/standard behavior as the defaults; each concrete subclass overrides
/// only what its dialect changes. Used directly (via {@link Dialects#forJooq}) for Postgres and any
/// dialect without specific quirks — Postgres natively supports {@code TIMESTAMP WITH TIME ZONE},
/// {@code RETURNING}, and snake_case identifiers, so these defaults already match it.
///
/// ## Why this exists
/// Dialect-variant behavior used to be scattered as inline `if (isMySQLFamily(...))` /
/// `switch (dialect)` conditionals across the conversion registry, {@code JooqUtil}, and
/// {@code JooqBackend}, with the same fact (e.g. "MySQL has no TIMESTAMP WITH TIME ZONE") duplicated
/// in two places that had to be kept in sync. This class holds the **defaults**; each concrete
/// subclass overrides only what its dialect changes. There is one cohesive place per dialect and no
/// scattered branching.
///
/// The four dialect-variant decisions:
/// - {@link #encodeCarrier} — the Java carrier type a SpeedyValue is encoded to (and thus which codec)
/// - {@link #sqlDataType} — the jOOQ {@link DataType} used to type a column reference
/// - {@link #supportsReturning} — whether {@code INSERT ... RETURNING} is available
/// - {@link #transformIdentifier} — how table/column identifiers are cased
///
/// Obtain instances via {@link Dialects#forJooq}; instances are stateless and shared as singletons.
public class DefaultDialect {

    /// Default Java carrier type a SpeedyValue is encoded to for a given ColumnType. An absent entry
    /// means the column type has no codec-backed carrier and the walker uses the SpeedyValue's
    /// primitive form. Subclasses override {@link #encodeCarrier} for dialect-specific carriers.
    private static final Map<ColumnType, Class<?>> DEFAULT_ENCODE_TYPE = Map.ofEntries(
            Map.entry(ColumnType.DATE, Date.class),
            Map.entry(ColumnType.TIME, Time.class),
            Map.entry(ColumnType.TIMESTAMP, Timestamp.class),
            Map.entry(ColumnType.TIMESTAMP_WITH_ZONE, OffsetDateTime.class),
            Map.entry(ColumnType.NUMERIC, BigDecimal.class),
            Map.entry(ColumnType.DECIMAL, BigDecimal.class),
            Map.entry(ColumnType.REAL, Double.class),
            Map.entry(ColumnType.BIGINT, BigInteger.class),
            Map.entry(ColumnType.TEXT, String.class),
            Map.entry(ColumnType.CLOB, String.class),
            Map.entry(ColumnType.VARCHAR, String.class),
            Map.entry(ColumnType.CHAR, String.class),
            Map.entry(ColumnType.UUID, UUID.class)
    );

    private final CodecRegistry registry = new CodecRegistry();

    public DefaultDialect() {
        registerDefaultCodecs(registry);
        registerDialectCodecs(registry);
    }

    // ---- codec registry ----

    /// Looks up the codec for a {@code (ColumnType, Java class)} in this dialect's registry.
    public Codec<?> findCodec(ColumnType col, Class<?> clazz) {
        return registry.findCodec(col, clazz);
    }

    /// The Java carrier type a SpeedyValue is encoded to for {@code col} under this dialect. Default
    /// behavior; subclasses override for dialect-specific carriers (e.g. MySQL zoned timestamps).
    public Class<?> encodeCarrier(ColumnType col) {
        return DEFAULT_ENCODE_TYPE.get(col);
    }

    /// Hook for subclasses to register or override codecs for their dialect. Default: nothing extra.
    protected void registerDialectCodecs(CodecRegistry r) {
    }

    /// The dialect-agnostic JDBC codecs. Each handles exactly one Java class — the JDBC driver's
    /// native type for the column — so there is no {@code instanceof} branching inside the lambdas.
    private void registerDefaultCodecs(CodecRegistry r) {
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
    }

    // ---- column typing ----

    /// The jOOQ {@link DataType} used to type a column reference for {@code col}. Default mapping;
    /// subclasses override entries their dialect renders differently.
    public DataType<?> sqlDataType(ColumnType col) {
        return switch (col) {
            case INTEGER -> SQLDataType.INTEGER;
            case SMALLINT -> SQLDataType.SMALLINT;
            case BIGINT -> SQLDataType.BIGINT;
            case DECIMAL -> SQLDataType.DECIMAL;
            case NUMERIC -> SQLDataType.NUMERIC;
            case FLOAT -> SQLDataType.FLOAT;
            case REAL -> SQLDataType.REAL;
            case DOUBLE -> SQLDataType.DOUBLE;
            case CHAR -> SQLDataType.CHAR;
            case VARCHAR, TEXT -> SQLDataType.VARCHAR;
            case DATE -> SQLDataType.DATE;
            case TIME -> SQLDataType.TIME;
            case TIMESTAMP -> SQLDataType.TIMESTAMP;
            case TIMESTAMP_WITH_ZONE -> SQLDataType.TIMESTAMPWITHTIMEZONE;
            case BOOLEAN -> SQLDataType.BOOLEAN;
            case BLOB -> SQLDataType.BLOB;
            case CLOB -> SQLDataType.CLOB;
            case UUID -> SQLDataType.UUID;
            default -> throw new RuntimeException("DataType not supported: " + col);
        };
    }

    // ---- statement / naming capabilities ----

    /// Whether the dialect supports {@code INSERT ... RETURNING} for reading generated keys back in
    /// the same round-trip. Default {@code true}; MySQL/MariaDB use {@code LAST_INSERT_ID()} instead.
    public boolean supportsReturning() {
        return true;
    }

    /// Transforms a Java identifier into the dialect's stored form. Default: camelCase → snake_case.
    public String transformIdentifier(String identifier) {
        return camelToSnake(identifier);
    }

    /// Converts a camelCase identifier to snake_case, inserting a separator at lower/digit-to-upper
    /// boundaries ({@code firstName -> first_name}, {@code address1Line -> address1_line}) and at
    /// acronym-to-word boundaries ({@code userIDCard -> user_id_card}), then lower-casing. A faithful
    /// replacement for the removed Spring {@code ParsingUtils.reconcatenateCamelCase}; the previous
    /// single-rule {@code ([a-z])([A-Z])} regex silently dropped the acronym and digit boundaries.
    protected static String camelToSnake(String identifier) {
        return identifier
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .toLowerCase();
    }
}
