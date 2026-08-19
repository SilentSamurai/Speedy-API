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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Locks the dialect-strategy mapping and the per-dialect overrides in one place — the behavior that
/// used to be scattered as `if (isMySQLFamily)` / `switch (dialect)` across the registry, JooqUtil,
/// and JooqBackend. Each subclass should override *only* what its dialect changes and inherit the rest.
class DialectsTest {

    @Test
    void forJooqMapsFamiliesToStrategies() {
        assertInstanceOf(MySqlDialect.class, Dialects.forJooq(SQLDialect.MYSQL));
        assertInstanceOf(MySqlDialect.class, Dialects.forJooq(SQLDialect.MARIADB));
        assertInstanceOf(UpperCaseIdentifierDialect.class, Dialects.forJooq(SQLDialect.H2));
        // MySqlDialect/UpperCaseIdentifierDialect are subtypes of DefaultDialect, so the default case
        // needs an exact-class check rather than instanceof.
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

    /// H2, HSQLDB, Derby and Firebird all fold unquoted identifiers to upper case, and the backend
    /// renders every name quoted — so mapping any of them to the snake_case default emits an identifier
    /// that matches nothing in the schema (#148).
    @Test
    void everyUpperCaseFoldingDialectUpperCasesIdentifiers() {
        for (SQLDialect dialect : List.of(SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.DERBY, SQLDialect.FIREBIRD)) {
            assertInstanceOf(UpperCaseIdentifierDialect.class, Dialects.forJooq(dialect), dialect.toString());
            assertEquals("FIRSTNAME", Dialects.forJooq(dialect).transformIdentifier("firstName"), dialect.toString());
        }
        // Casing is all Derby and Firebird are known to share with H2, so they share its instance.
        assertSame(Dialects.forJooq(SQLDialect.H2), Dialects.forJooq(SQLDialect.DERBY));
        assertSame(Dialects.forJooq(SQLDialect.H2), Dialects.forJooq(SQLDialect.FIREBIRD));
    }

    @Test
    void upperCaseIdentifierDialectOnlyOverridesIdentifierCasing() {
        DefaultDialect d = Dialects.forJooq(SQLDialect.H2);
        assertEquals("FIRSTNAME", d.transformIdentifier("firstName"));
        // Everything else keeps the defaults.
        assertEquals(SQLDataType.TIMESTAMPWITHTIMEZONE, d.sqlDataType(ColumnType.TIMESTAMP_WITH_ZONE));
        assertEquals(OffsetDateTime.class, d.encodeCarrier(ColumnType.TIMESTAMP_WITH_ZONE));
        assertTrue(d.supportsReturning());
    }

    /// HSQLDB needs the casing *and* two storage overrides, because of the DDL Hibernate emits for it:
    /// a ZonedDateTime field becomes a bare `timestamp(6)` and a UUID field becomes `binary(16)`.
    @Test
    void hsqldbAddsZonedAndUuidStorageOverridesOnTopOfTheCasing() {
        DefaultDialect d = Dialects.forJooq(SQLDialect.HSQLDB);
        assertInstanceOf(HsqldbDialect.class, d);
        assertEquals("FIRSTNAME", d.transformIdentifier("firstName"));

        assertEquals(LocalDateTime.class, d.encodeCarrier(ColumnType.TIMESTAMP_WITH_ZONE));
        assertEquals(SQLDataType.LOCALDATETIME, d.sqlDataType(ColumnType.TIMESTAMP_WITH_ZONE));
        assertEquals(byte[].class, d.encodeCarrier(ColumnType.UUID));
        assertEquals(SQLDataType.BINARY.length(16), d.sqlDataType(ColumnType.UUID));

        // A storage override registers its codec, so the carrier round-trips.
        assertNotNull(d.findCodec(ColumnType.UUID, byte[].class));
        assertNotNull(d.findCodec(ColumnType.TIMESTAMP_WITH_ZONE, LocalDateTime.class));

        // Untouched column types and the remaining capabilities keep the defaults.
        assertEquals(SQLDataType.INTEGER, d.sqlDataType(ColumnType.INTEGER));
        assertTrue(d.supportsReturning());
    }

    /// The same declaration drives MySQL's zoned-timestamp storage, so the two dialects that share the
    /// fact share the value rather than repeating three agreeing overrides each.
    @Test
    void mySqlAndHsqldbShareTheZonedStorageDeclaration() {
        DefaultDialect mysql = Dialects.forJooq(SQLDialect.MYSQL);
        DefaultDialect hsqldb = Dialects.forJooq(SQLDialect.HSQLDB);
        assertEquals(mysql.encodeCarrier(ColumnType.TIMESTAMP_WITH_ZONE),
                hsqldb.encodeCarrier(ColumnType.TIMESTAMP_WITH_ZONE));
        assertEquals(mysql.sqlDataType(ColumnType.TIMESTAMP_WITH_ZONE),
                hsqldb.sqlDataType(ColumnType.TIMESTAMP_WITH_ZONE));
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
