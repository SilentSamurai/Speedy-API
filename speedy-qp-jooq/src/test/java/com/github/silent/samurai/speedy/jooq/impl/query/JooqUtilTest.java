package com.github.silent.samurai.speedy.jooq.impl.query;

import org.jooq.SQLDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JooqUtilTest {

    @Test
    void h2UpperCasesIdentifier() {
        assertEquals("FIRSTNAME", JooqUtil.transformIdentifier("firstName", SQLDialect.H2));
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
