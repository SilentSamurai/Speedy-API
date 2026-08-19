package com.github.silent.samurai.speedy.jooq.impl.query;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Runs the backend's own table/column references against a real embedded HSQLDB, over a schema
/// created with unquoted DDL — the setup issue #148 was reported on.
///
/// The unit gate otherwise runs against H2 only, so a dialect mapped to the wrong identifier casing
/// is invisible in it: every name is rendered quoted ({@code RenderQuotedNames.ALWAYS} +
/// {@code RenderNameStyle.AS_IS}), and a quoted identifier is matched verbatim, so the mismatch shows
/// up only when a database is asked to resolve it. HSQLDB is embedded like H2, so this needs no
/// server and no CI service — it just runs.
///
/// Before the fix both tests failed with `user lacks privilege or object not found: vendor`, because
/// HSQLDB fell through to the snake_case default and the query asked for {@code "vendor"} — verified
/// by mapping HSQLDB back to the default strategy and re-running.
class HsqldbIdentifierCasingTest {

    /// The settings JooqBackend renders with. Quoted names are the reason casing has to be right.
    private static final Settings SETTINGS = new Settings()
            .withRenderQuotedNames(RenderQuotedNames.ALWAYS)
            .withRenderNameStyle(RenderNameStyle.AS_IS);

    private static Connection connection;

    @BeforeAll
    static void createSchema() throws SQLException {
        connection = DriverManager.getConnection("jdbc:hsqldb:mem:speedy-identifier-casing", "sa", "");
        try (Statement statement = connection.createStatement()) {
            // Unquoted DDL, as Liquibase/Hibernate emit it: HSQLDB stores these names upper-cased.
            statement.execute("CREATE TABLE VENDOR (ID VARCHAR(36) NOT NULL PRIMARY KEY, NAME VARCHAR(50))");
            statement.execute("INSERT INTO VENDOR (ID, NAME) VALUES ('v-1', 'Acme')");
        }
    }

    @AfterAll
    static void dropSchema() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SHUTDOWN");
        }
        connection.close();
    }

    @Test
    void tableReferenceResolvesAgainstTheStoredSchema() {
        int count = dsl().selectCount()
                .from(JooqUtil.getTable(vendorEntity(), SQLDialect.HSQLDB))
                .fetchOne(0, int.class);

        assertEquals(1, count);
    }

    @Test
    void columnReferenceResolvesAgainstTheStoredSchema() {
        FieldMetadata name = vendorField("NAME");

        String value = dsl().select(JooqUtil.<String>getColumn(name, SQLDialect.HSQLDB))
                .from(JooqUtil.getTable(name.getEntityMetadata(), SQLDialect.HSQLDB))
                .fetchOne(0, String.class);

        assertEquals("Acme", value);
    }

    private static DSLContext dsl() {
        return DSL.using(connection, SQLDialect.HSQLDB, SETTINGS);
    }

    private static EntityMetadata vendorEntity() {
        EntityMetadata entityMetadata = mock(EntityMetadata.class);
        when(entityMetadata.getDbTableName()).thenReturn("VENDOR");
        return entityMetadata;
    }

    private static FieldMetadata vendorField(String dbColumnName) {
        // Built before the stubbing below: mocking inside a thenReturn(...) argument leaves Mockito
        // mid-stub and fails as UnfinishedStubbing.
        EntityMetadata entityMetadata = vendorEntity();
        FieldMetadata fieldMetadata = mock(FieldMetadata.class);
        when(fieldMetadata.getDbColumnName()).thenReturn(dbColumnName);
        when(fieldMetadata.getColumnType()).thenReturn(ColumnType.VARCHAR);
        when(fieldMetadata.getEntityMetadata()).thenReturn(entityMetadata);
        return fieldMetadata;
    }
}
