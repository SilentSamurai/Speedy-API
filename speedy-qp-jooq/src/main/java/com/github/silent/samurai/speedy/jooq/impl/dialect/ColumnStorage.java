package com.github.silent.samurai.speedy.jooq.impl.dialect;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.jooq.impl.conversion.CodecRegistry;
import com.github.silent.samurai.speedy.models.SpeedyText;
import com.github.silent.samurai.speedy.utils.Speedy;
import org.jooq.DataType;
import org.jooq.impl.SQLDataType;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.function.Consumer;

/// How one {@link ColumnType} is physically stored when a dialect doesn't store it the standard way.
///
/// A single underlying fact — "this dialect has no `TIMESTAMP WITH TIME ZONE`" — used to require three
/// separate overrides per dialect ({@link DefaultDialect#encodeCarrier}, {@link DefaultDialect#sqlDataType},
/// and a codec registration) that had to agree with each other, and had to be repeated in full by every
/// dialect sharing the fact. Bundling the three into one value lets a dialect *declare* its storage
/// differences instead of implementing them, and lets two unrelated dialects share the same declaration.
///
/// @param carrier       the Java type a SpeedyValue is encoded to for this column
/// @param dataType      the jOOQ type used to type a reference to the column
/// @param registerCodec registers the codec converting between {@code carrier} and SpeedyValue
public record ColumnStorage(Class<?> carrier, DataType<?> dataType, Consumer<CodecRegistry> registerCodec) {

    /// A zoned timestamp in a column that has no time zone (MySQL/MariaDB have no
    /// `TIMESTAMP WITH TIME ZONE` at all; Hibernate maps one to a bare `timestamp` on HSQLDB).
    /// The instant is normalised to UTC on the way in and read back as UTC, so the stored wall-clock
    /// denotes the same instant — a plain `toLocalDateTime()` would drop the offset and shift it.
    public static final ColumnStorage ZONED_AS_LOCAL_UTC = new ColumnStorage(
            LocalDateTime.class,
            SQLDataType.LOCALDATETIME,
            r -> r.register(ColumnType.TIMESTAMP_WITH_ZONE, LocalDateTime.class,
                    sv -> sv.asZonedDateTime().withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(),
                    raw -> Speedy.from(raw.atZone(ZoneOffset.UTC))));

    /// A UUID in a 16-byte binary column, which is what Hibernate's DDL emits for a `java.util.UUID`
    /// field on dialects without a native UUID type. Writing the textual form into such a column fails
    /// (HSQLDB: `data exception: invalid character value for cast`), so the value is carried as its
    /// raw bytes and rendered back as the canonical dashed string on read.
    public static final ColumnStorage UUID_AS_BINARY_16 = new ColumnStorage(
            byte[].class,
            SQLDataType.BINARY.length(16),
            r -> r.register(ColumnType.UUID, byte[].class,
                    sv -> toBytes(UUID.fromString(((SpeedyText) sv).asText())),
                    raw -> Speedy.from(toUuid(raw).toString())));

    private static byte[] toBytes(UUID uuid) {
        return ByteBuffer.allocate(16)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits())
                .array();
    }

    private static UUID toUuid(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }
}
