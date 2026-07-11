package com.github.silent.samurai.speedy.jooq.impl;

import com.github.silent.samurai.speedy.jooq.impl.dialect.DefaultDialect;
import com.github.silent.samurai.speedy.jooq.impl.dialect.H2Dialect;
import com.github.silent.samurai.speedy.jooq.impl.dialect.MySqlDialect;
import org.jooq.SQLDialect;

/// Maps a jOOQ {@link SQLDialect} to its {@link DefaultDialect} strategy. Strategies are stateless
/// after construction (their codec registries are populated once and only read afterwards), so a
/// single shared instance per family is safe for concurrent use.
public final class Dialects {

    private static final DefaultDialect MYSQL = new MySqlDialect();
    private static final DefaultDialect H2 = new H2Dialect();
    private static final DefaultDialect DEFAULT = new DefaultDialect();

    private Dialects() {
    }

    public static DefaultDialect forJooq(SQLDialect dialect) {
        return switch (dialect) {
            case MYSQL, MARIADB -> MYSQL;
            case H2 -> H2;
            default -> DEFAULT;
        };
    }
}
