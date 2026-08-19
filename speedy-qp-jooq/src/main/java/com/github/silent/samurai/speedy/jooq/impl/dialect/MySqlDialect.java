package com.github.silent.samurai.speedy.jooq.impl.dialect;

import com.github.silent.samurai.speedy.enums.ColumnType;

import java.util.Map;

/// MySQL and MariaDB dialect strategy. The single underlying fact — neither supports
/// {@code TIMESTAMP WITH TIME ZONE} — is declared as a storage override, and it also costs them
/// {@code RETURNING}, so generated keys are read via {@code LAST_INSERT_ID()} instead. Identifier
/// casing follows the snake_case default.
public final class MySqlDialect extends DefaultDialect {

    public MySqlDialect() {
        super(Map.of(ColumnType.TIMESTAMP_WITH_ZONE, ColumnStorage.ZONED_AS_LOCAL_UTC));
    }

    @Override
    public boolean supportsReturning() {
        return false;
    }
}
