package com.github.silent.samurai.speedy.jooq.impl.query;

import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JooqUtilTest {

    /// Every dialect that folds unquoted identifiers to upper case needs the same treatment: the
    /// backend renders all names quoted, so a lower-cased name can never match the stored one.
    @Test
    void upperCaseFoldingDialectsUpperCaseIdentifiers() {
        for (SQLDialect dialect : List.of(SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.DERBY, SQLDialect.FIREBIRD)) {
            assertEquals("FIRSTNAME", JooqUtil.transformIdentifier("firstName", dialect), dialect.toString());
            assertEquals("VENDOR", JooqUtil.transformIdentifier("VENDOR", dialect), dialect.toString());
        }
    }

    /// The failure reported in #148, at the level it was observed: HSQLDB stores unquoted DDL names
    /// upper-cased, and the backend's `RenderQuotedNames.ALWAYS` + `RenderNameStyle.AS_IS` settings
    /// mean a lower-cased identifier is emitted as `"vendor"` and never resolves.
    @Test
    void hsqldbCountQueryRendersTheStoredUpperCaseTableName() {
        Settings settings = new Settings()
                .withRenderQuotedNames(RenderQuotedNames.ALWAYS)
                .withRenderNameStyle(RenderNameStyle.AS_IS);
        String sql = DSL.using(SQLDialect.HSQLDB, settings)
                .selectCount()
                .from(DSL.table(DSL.name(JooqUtil.transformIdentifier("VENDOR", SQLDialect.HSQLDB))))
                .getSQL();
        assertEquals("select count(*) from \"VENDOR\"", sql);
    }

    @Test
    void simpleCamelCaseBecomesSnakeCase() {
        assertEquals("first_name", JooqUtil.transformIdentifier("firstName", SQLDialect.POSTGRES));
    }

    @Test
    void trailingAcronymBecomesSnakeCase() {
        assertEquals("user_id", JooqUtil.transformIdentifier("userID", SQLDialect.POSTGRES));
    }

    /// Acronym immediately followed by a word — the boundary the previous single-rule regex dropped
    /// ({@code userIDCard -> user_idcard} instead of {@code user_id_card}).
    @Test
    void acronymFollowedByWordIsSplit() {
        assertEquals("user_id_card", JooqUtil.transformIdentifier("userIDCard", SQLDialect.POSTGRES));
        assertEquals("html_parser", JooqUtil.transformIdentifier("HTMLParser", SQLDialect.POSTGRES));
    }

    /// Digit immediately followed by an upper-case letter — also dropped by the previous regex
    /// ({@code address1Line -> address1line} instead of {@code address1_line}).
    @Test
    void digitToUpperBoundaryIsSplit() {
        assertEquals("address1_line", JooqUtil.transformIdentifier("address1Line", SQLDialect.POSTGRES));
    }

    @Test
    void alreadySnakeCaseIsUnchanged() {
        assertEquals("first_name", JooqUtil.transformIdentifier("first_name", SQLDialect.POSTGRES));
    }
}
