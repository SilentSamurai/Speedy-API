package com.github.silent.samurai.speedy.file.impl.models;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public class JsonEntity {

    @NotNull
    public String name;

    public List<JsonField> fields;

    @NotNull
    public boolean hasCompositeKey;

    public boolean sensitive = false;

    public String dbTable;
    public String keyType;
    public String transactionMode;

    // Nullable so that an absent key leaves the metadata default (false) intact,
    // while an explicit `"bulkAllowed": true` enables bulk create/delete.
    public Boolean bulkAllowed;

}
