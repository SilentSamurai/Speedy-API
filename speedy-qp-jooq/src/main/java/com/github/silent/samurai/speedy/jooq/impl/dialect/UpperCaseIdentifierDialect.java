package com.github.silent.samurai.speedy.jooq.impl.dialect;

/// Strategy for every dialect that folds unquoted identifiers to upper case — H2, HSQLDB, Derby and
/// Firebird. They match the defaults for conversion, column typing and {@code RETURNING}; they differ
/// only in identifier casing, so they share one strategy rather than one empty subclass each.
///
/// The casing matters because the backend renders every name quoted
/// ({@code RenderQuotedNames.ALWAYS} + {@code RenderNameStyle.AS_IS}): a quoted identifier is matched
/// verbatim, so no case folding can rescue a name emitted in the wrong form. Against a schema created
/// with unquoted DDL ({@code CREATE TABLE VENDOR (...)}) the stored name is {@code VENDOR}, and the
/// snake_case default would emit {@code "vendor"} — never resolving.
///
/// A dialect that later needs a quirk of its own gets its own subclass then.
public class UpperCaseIdentifierDialect extends DefaultDialect {

    @Override
    public String transformIdentifier(String identifier) {
        return identifier.toUpperCase();
    }
}
