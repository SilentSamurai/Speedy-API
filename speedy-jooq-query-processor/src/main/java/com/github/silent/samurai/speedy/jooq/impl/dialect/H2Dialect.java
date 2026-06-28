package com.github.silent.samurai.speedy.jooq.impl.dialect;

/// H2 dialect strategy. H2 matches the defaults for conversion, column typing, and {@code RETURNING};
/// it differs only in identifier casing — Speedy stores H2 identifiers upper-cased rather than
/// snake_cased.
public final class H2Dialect extends DefaultDialect {

    @Override
    public String transformIdentifier(String identifier) {
        return identifier.toUpperCase();
    }
}
