package com.github.silent.samurai.speedy.jooq.impl;

import com.github.silent.samurai.speedy.jooq.impl.dialect.DefaultDialect;
import com.github.silent.samurai.speedy.jooq.impl.dialect.HsqldbDialect;
import com.github.silent.samurai.speedy.jooq.impl.dialect.MySqlDialect;
import com.github.silent.samurai.speedy.jooq.impl.dialect.UpperCaseIdentifierDialect;
import org.jooq.SQLDialect;

/// Maps a jOOQ {@link SQLDialect} to its {@link DefaultDialect} strategy. Strategies are stateless
/// after construction (their codec registries are populated once and only read afterwards), so a
/// single shared instance per family is safe for concurrent use.
public final class Dialects {

    private static final DefaultDialect MYSQL = new MySqlDialect();
    private static final DefaultDialect UPPER_CASE_IDENTIFIERS = new UpperCaseIdentifierDialect();
    private static final DefaultDialect HSQLDB = new HsqldbDialect();
    private static final DefaultDialect DEFAULT = new DefaultDialect();

    private Dialects() {
    }

    public static DefaultDialect forJooq(SQLDialect dialect) {
        return switch (dialect) {
            case MYSQL, MARIADB -> MYSQL;
            case HSQLDB -> HSQLDB;
            // Dialects that fold unquoted identifiers to upper case. Every name is rendered quoted, so
            // one of these mapped to the snake_case default resolves against nothing at all.
            case H2, DERBY, FIREBIRD -> UPPER_CASE_IDENTIFIERS;
            // Postgres and SQLite are correct here — Postgres folds unquoted DDL to lower case, and
            // SQLite matches identifiers case-insensitively. Any *other* dialect reaching this arm gets
            // snake_case by assumption, not by verification: check its folding rules before using it.
            default -> DEFAULT;
        };
    }
}
