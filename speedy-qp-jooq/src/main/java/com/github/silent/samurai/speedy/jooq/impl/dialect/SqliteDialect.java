package com.github.silent.samurai.speedy.jooq.impl.dialect;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.utils.Speedy;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

/// SQLite dialect strategy. SQLite has five storage classes — NULL, INTEGER, REAL, TEXT and BLOB —
/// and no temporal, UUID or fixed-width float type at all, so every column type that has no storage
/// class of its own is declared here as text.
///
/// Identifier casing needs no override: SQLite compares identifiers case-insensitively, so the
/// snake_case default already resolves.
///
/// The text form matters because Speedy is not the only writer. Hibernate creates the schema and
/// seeds rows through the same JDBC driver, and the driver's `date_class=text` setting stores
/// `java.sql` temporal values as `yyyy-MM-dd HH:mm:ss.SSS` — the form parsed here. Without that
/// setting the driver stores them as epoch-millis INTEGERs instead, which no reader can interpret as
/// a timestamp without knowing that convention, so the `sqlite` profile sets it in the JDBC URL.
///
/// The columns are typed VARCHAR rather than DATE/TIMESTAMP so jOOQ does no parsing of its own —
/// its default binding calls `Timestamp.valueOf` and fails on anything but one exact format. Text in
/// this form also sorts and compares chronologically, so ORDER BY and range filters still hold.
public final class SqliteDialect extends DefaultDialect {

    /// Accepted on read, most specific first. Hibernate, the seed script and Speedy itself write
    /// slightly different widths — with and without fractional seconds, and `CURRENT_TIMESTAMP`
    /// yields whole seconds — so reading tolerates each of them.
    private static final DateTimeFormatter[] DATE_TIME_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    /// Written on encode. The driver stores *every* `java.sql` temporal as a full
    /// `yyyy-MM-dd HH:mm:ss.SSS` timestamp — a DATE as `2021-01-02 00:00:00.000`, a TIME as
    /// `1970-01-01 03:04:05.000` — and rejects anything narrower on read ("Error parsing date").
    /// Hibernate reads the same rows through that driver, so Speedy has to write the same widths.
    private static final DateTimeFormatter DATE_TIME_OUT = DATE_TIME_FORMATS[0];
    private static final DateTimeFormatter DATE_OUT = DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00.000");
    private static final DateTimeFormatter TIME_OUT = DateTimeFormatter.ofPattern("'1970-01-01' HH:mm:ss.SSS");

    /// Parses the date part out of any of the accepted widths.
    private static final DateTimeFormatter DATE_IN = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final ColumnStorage DATE_AS_TEXT = new ColumnStorage(
            String.class, SQLDataType.VARCHAR,
            r -> r.register(ColumnType.DATE, String.class,
                    sv -> sv.asDate().format(DATE_OUT),
                    raw -> Speedy.from(parseDate(raw))));

    private static final ColumnStorage TIME_AS_TEXT = new ColumnStorage(
            String.class, SQLDataType.VARCHAR,
            r -> r.register(ColumnType.TIME, String.class,
                    sv -> sv.asTime().format(TIME_OUT),
                    raw -> Speedy.from(parseTime(raw))));

    private static final ColumnStorage TIMESTAMP_AS_TEXT = new ColumnStorage(
            String.class, SQLDataType.VARCHAR,
            r -> r.register(ColumnType.TIMESTAMP, String.class,
                    sv -> sv.asDateTime().format(DATE_TIME_OUT),
                    raw -> Speedy.from(parseDateTime(raw))));

    /// A zoned timestamp normalised to UTC before it is written, and read back as UTC — the same
    /// treatment {@link ColumnStorage#ZONED_AS_LOCAL_UTC} gives dialects with no zoned type, except
    /// that the carrier is text. Storing the offset instead would break ordering across offsets.
    private static final ColumnStorage ZONED_AS_UTC_TEXT = new ColumnStorage(
            String.class, SQLDataType.VARCHAR,
            r -> r.register(ColumnType.TIMESTAMP_WITH_ZONE, String.class,
                    sv -> sv.asZonedDateTime().withZoneSameInstant(ZoneOffset.UTC)
                            .toLocalDateTime().format(DATE_TIME_OUT),
                    raw -> Speedy.from(parseDateTime(raw).atZone(ZoneOffset.UTC))));

    /// Hibernate declares a `java.util.UUID` field as `blob` on SQLite, so the value travels as its
    /// raw bytes — the same carrier HSQLDB needs for its `binary(16)`, under SQLite's type.
    private static final ColumnStorage UUID_AS_BLOB = ColumnStorage.uuidAsBinary(SQLDataType.BLOB);

    /// SQLite's only floating-point storage class is an 8-byte double, and Hibernate declares a Java
    /// `Double` field as `float` on it. Carrying that column as a 4-byte float — the default for
    /// ColumnType.FLOAT — would lose precision on the way in for no reason (90.9 reads back as
    /// 90.9000015258789).
    private static final ColumnStorage FLOAT_AS_DOUBLE = new ColumnStorage(
            Double.class, SQLDataType.DOUBLE,
            r -> r.register(ColumnType.FLOAT, Double.class,
                    sv -> sv.asDouble(),
                    Speedy::from));

    /// SQLite's primary result code for a constraint violation. Its extended codes carry that value
    /// in the low byte and the specific constraint in the next one — SQLITE_CONSTRAINT_NOTNULL is
    /// 1299, SQLITE_CONSTRAINT_UNIQUE 2067 — so the family is matched by masking.
    private static final int SQLITE_CONSTRAINT = 19;

    /// The column types stored as text above — the ones whose comparison form is not the database's
    /// to guarantee.
    private static final Set<ColumnType> TEXT_TEMPORALS = Set.of(
            ColumnType.DATE, ColumnType.TIME, ColumnType.TIMESTAMP, ColumnType.TIMESTAMP_WITH_ZONE);

    public SqliteDialect() {
        super(Map.of(
                ColumnType.DATE, DATE_AS_TEXT,
                ColumnType.TIME, TIME_AS_TEXT,
                ColumnType.TIMESTAMP, TIMESTAMP_AS_TEXT,
                ColumnType.TIMESTAMP_WITH_ZONE, ZONED_AS_UTC_TEXT,
                ColumnType.UUID, UUID_AS_BLOB,
                ColumnType.FLOAT, FLOAT_AS_DOUBLE
        ));
    }

    /// Temporal columns are text here, so a comparison is a text comparison — and it only holds if
    /// every value in the column has the same width. Speedy writes `yyyy-MM-dd HH:mm:ss.SSS`, but a
    /// column carrying `DEFAULT CURRENT_TIMESTAMP` is written by SQLite itself, which produces whole
    /// seconds. The shorter form is a prefix of the longer one and sorts before it, so `$eq` matched
    /// nothing and a range filter dropped the row.
    ///
    /// `strftime` reads any width SQLite accepts as a time string and renders the one Speedy writes,
    /// so both sides of the comparison are in the same form. It costs an index scan on the column,
    /// which is the price of a storage class SQLite does not have.
    @Override
    public Field<Object> comparisonField(Field<Object> column, ColumnType columnType) {
        if (!TEXT_TEMPORALS.contains(columnType)) {
            return column;
        }
        return DSL.field("strftime('%Y-%m-%d %H:%M:%f', {0})", Object.class, column);
    }

    /// The driver reports no standard SQLSTATE, so a constraint violation is recognisable only by its
    /// result code. Without this a rejected write surfaces as a 500 instead of a 400.
    @Override
    public boolean isClientErrorCode(int errorCode) {
        return (errorCode & 0xFF) == SQLITE_CONSTRAINT;
    }

    private static LocalDateTime parseDateTime(String raw) {
        String text = raw.trim();
        for (DateTimeFormatter format : DATE_TIME_FORMATS) {
            try {
                return LocalDateTime.parse(text, format);
            } catch (Exception ignored) {
                // try the next accepted width
            }
        }
        // A date-only value in a timestamp column: midnight, as every other backend reads it.
        return parseDate(text).atStartOfDay();
    }

    private static LocalDate parseDate(String raw) {
        String text = raw.trim();
        // The driver writes a DATE as a full timestamp, so take the date part when one is present.
        int split = text.indexOf(' ');
        if (split < 0) {
            split = text.indexOf('T');
        }
        return LocalDate.parse(split < 0 ? text : text.substring(0, split), DATE_IN);
    }

    private static LocalTime parseTime(String raw) {
        String text = raw.trim();
        // The driver writes a TIME as a full timestamp; keep only the time part when one is present.
        int split = text.indexOf(' ');
        if (split >= 0) {
            text = text.substring(split + 1);
        }
        return LocalTime.parse(text);
    }
}
