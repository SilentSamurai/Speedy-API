package com.github.silent.samurai.speedy.jooq.impl.dialect;

import com.github.silent.samurai.speedy.enums.ColumnType;

import java.util.Map;

/// HSQLDB dialect strategy. Identifiers fold to upper case (inherited), and two column types are
/// stored differently from the ANSI defaults — both because of the DDL Hibernate emits for HSQLDB
/// rather than because HSQLDB lacks the type:
/// - a `ZonedDateTime` field becomes a bare `timestamp(6)`, so the instant is carried as UTC local time
/// - a `java.util.UUID` field becomes `binary(16)`, so the value is carried as its raw bytes
public final class HsqldbDialect extends UpperCaseIdentifierDialect {

    public HsqldbDialect() {
        super(Map.of(
                ColumnType.TIMESTAMP_WITH_ZONE, ColumnStorage.ZONED_AS_LOCAL_UTC,
                ColumnType.UUID, ColumnStorage.UUID_AS_BINARY_16
        ));
    }
}
