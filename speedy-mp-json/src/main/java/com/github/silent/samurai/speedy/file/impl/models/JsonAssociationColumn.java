package com.github.silent.samurai.speedy.file.impl.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/// One column of a multi-column foreign key declared in a JSON metamodel: the column on the
/// *owning* entity's table ({@code dbColumn}), paired with the key field of the *target* entity it
/// references ({@code associatedColumn} — the same name the singular form uses for the target
/// field). Used via {@link JsonField#associatedColumns}; one entry per column of the target's
/// primary key, in the target's key-field order.
public class JsonAssociationColumn {

    @JsonProperty(required = true)
    public String dbColumn;

    @JsonProperty(required = true)
    public String associatedColumn;

}
