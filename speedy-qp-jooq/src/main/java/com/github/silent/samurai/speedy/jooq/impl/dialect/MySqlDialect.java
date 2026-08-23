package com.github.silent.samurai.speedy.jooq.impl.dialect;

import com.github.silent.samurai.speedy.enums.ColumnType;

import java.util.Map;

/// MySQL and MariaDB dialect strategy. The single underlying fact — neither supports
/// {@code TIMESTAMP WITH TIME ZONE} — is declared as a storage override, and it also costs them
/// {@code RETURNING}, so generated keys are read via {@code LAST_INSERT_ID()} instead. Identifier
/// casing follows the snake_case default.
public final class MySqlDialect extends DefaultDialect {

    /// MySQL "Field '%s' doesn't have a default value".
    private static final int ER_NO_DEFAULT_FOR_FIELD = 1364;
    /// MySQL "Incorrect %s value: '%s' for column '%s'".
    private static final int ER_TRUNCATED_WRONG_VALUE_FOR_FIELD = 1366;

    public MySqlDialect() {
        super(Map.of(ColumnType.TIMESTAMP_WITH_ZONE, ColumnStorage.ZONED_AS_LOCAL_UTC));
    }

    @Override
    public boolean supportsReturning() {
        return false;
    }

    /// MySQL/MariaDB report a missing required column (no default) and a wrong-typed value under the
    /// generic HY000 state, where H2 and Postgres use 22/23. Both are bad client input.
    @Override
    public boolean isClientErrorCode(int errorCode) {
        return errorCode == ER_NO_DEFAULT_FOR_FIELD || errorCode == ER_TRUNCATED_WRONG_VALUE_FOR_FIELD;
    }
}
