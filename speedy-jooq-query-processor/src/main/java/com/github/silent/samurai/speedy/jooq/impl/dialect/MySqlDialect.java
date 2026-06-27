package com.github.silent.samurai.speedy.jooq.impl.dialect;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.jooq.impl.conversion.CodecRegistry;
import com.github.silent.samurai.speedy.utils.Speedy;
import org.jooq.DataType;
import org.jooq.impl.SQLDataType;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/// MySQL and MariaDB dialect strategy. The single underlying fact — neither supports
/// {@code TIMESTAMP WITH TIME ZONE} — drives three overrides plus a dialect codec:
/// a zoned timestamp is carried as a bare {@link LocalDateTime} normalised to UTC, the column is
/// typed as {@code LOCALDATETIME}, and generated keys are read via {@code LAST_INSERT_ID()} rather
/// than {@code RETURNING}. Identifier casing follows the snake_case default.
public final class MySqlDialect extends DefaultDialect {

    @Override
    protected void registerDialectCodecs(CodecRegistry r) {
        // Normalise to UTC on encode (matching the UTC assumption on decode) so the stored wall-clock
        // represents the same instant; a plain toLocalDateTime() would drop the offset and shift it.
        r.register(ColumnType.TIMESTAMP_WITH_ZONE, LocalDateTime.class,
                sv -> sv.asZonedDateTime().withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(),
                raw -> Speedy.from(raw.atZone(ZoneOffset.UTC)));
    }

    @Override
    public Class<?> encodeCarrier(ColumnType col) {
        if (col == ColumnType.TIMESTAMP_WITH_ZONE) {
            return LocalDateTime.class;
        }
        return super.encodeCarrier(col);
    }

    @Override
    public DataType<?> sqlDataType(ColumnType col) {
        if (col == ColumnType.TIMESTAMP_WITH_ZONE) {
            return SQLDataType.LOCALDATETIME;
        }
        return super.sqlDataType(col);
    }

    @Override
    public boolean supportsReturning() {
        return false;
    }
}
