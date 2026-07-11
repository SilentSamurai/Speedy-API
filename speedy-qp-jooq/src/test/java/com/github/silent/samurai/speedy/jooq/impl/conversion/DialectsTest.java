package com.github.silent.samurai.speedy.jooq.impl.conversion;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.jooq.impl.Dialects;
import com.github.silent.samurai.speedy.jooq.impl.dialect.*;
import org.jooq.SQLDialect;
import org.jooq.impl.SQLDataType;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

/// Locks the dialect-strategy mapping and the per-dialect overrides in one place — the behavior that
/// used to be scattered as `if (isMySQLFamily)` / `switch (dialect)` across the registry, JooqUtil,
/// and JooqBackend. Each subclass should override *only* what its dialect changes and inherit the rest.
class DialectsTest {

    @Test
    void forJooqMapsFamiliesToStrategies() {
        assertInstanceOf(MySqlDialect.class, Dialects.forJooq(SQLDialect.MYSQL));
        assertInstanceOf(MySqlDialect.class, Dialects.forJooq(SQLDialect.MARIADB));
        assertInstanceOf(H2Dialect.class, Dialects.forJooq(SQLDialect.H2));
        // MySqlDialect/H2Dialect are subtypes of DefaultDialect, so the default case needs an
        // exact-class check rather than instanceof.
        assertEquals(DefaultDialect.class, Dialects.forJooq(SQLDialect.POSTGRES).getClass());
        assertEquals(DefaultDialect.class, Dialects.forJooq(SQLDialect.SQLITE).getClass());
        // MySQL and MariaDB share one strategy instance.
        assertSame(Dialects.forJooq(SQLDialect.MYSQL), Dialects.forJooq(SQLDialect.MARIADB));
    }

    @Test
    void mySqlOverridesTimestampWithZoneAndReturning() {
        DefaultDialect d = Dialects.forJooq(SQLDialect.MYSQL);
        // The three overrides driven by "MySQL has no TIMESTAMP WITH TIME ZONE".
        assertEquals(LocalDateTime.class, d.encodeCarrier(ColumnType.TIMESTAMP_WITH_ZONE));
        assertEquals(SQLDataType.LOCALDATETIME, d.sqlDataType(ColumnType.TIMESTAMP_WITH_ZONE));
        assertFalse(d.supportsReturning());
        // Everything else falls through to the base defaults.
        assertEquals(Timestamp.class, d.encodeCarrier(ColumnType.TIMESTAMP));
        assertEquals(SQLDataType.INTEGER, d.sqlDataType(ColumnType.INTEGER));
        assertEquals("first_name", d.transformIdentifier("firstName"));
    }

    @Test
    void h2OnlyOverridesIdentifierCasing() {
        DefaultDialect d = Dialects.forJooq(SQLDialect.H2);
        assertEquals("FIRSTNAME", d.transformIdentifier("firstName"));
        // H2 keeps the defaults for everything else.
        assertEquals(SQLDataType.TIMESTAMPWITHTIMEZONE, d.sqlDataType(ColumnType.TIMESTAMP_WITH_ZONE));
        assertEquals(OffsetDateTime.class, d.encodeCarrier(ColumnType.TIMESTAMP_WITH_ZONE));
        assertTrue(d.supportsReturning());
    }

    @Test
    void defaultDialectUsesAllDefaults() {
        DefaultDialect d = Dialects.forJooq(SQLDialect.POSTGRES);
        assertEquals("first_name", d.transformIdentifier("firstName"));
        assertEquals(OffsetDateTime.class, d.encodeCarrier(ColumnType.TIMESTAMP_WITH_ZONE));
        assertEquals(SQLDataType.TIMESTAMPWITHTIMEZONE, d.sqlDataType(ColumnType.TIMESTAMP_WITH_ZONE));
        assertTrue(d.supportsReturning());
    }
}
