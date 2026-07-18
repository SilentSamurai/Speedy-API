package com.github.silent.samurai.speedy.file.impl.models;

import java.util.List;

public class JsonEntity {

    public String name;

    public List<JsonField> fields;

    public boolean hasCompositeKey;

    public boolean sensitive = false;

    public String dbTable;
    public String keyType;

    // The write operations that accept bulk (multi-element) bodies, e.g. ["CREATE","DELETE"]
    // or ["ALL"]. Values are BulkOperation names (case-insensitive). Absent/empty leaves the
    // metadata default (bulk disabled) intact.
    public List<String> bulk;

    /// Legacy all-or-nothing bulk flag. When {@code bulk} is absent, {@code true} is treated
    /// as {@code ["ALL"]}; {@code false} retains the default of no bulk operations.
    ///
    /// @deprecated Use {@link #bulk} to declare the allowed operations explicitly.
    @Deprecated
    public Boolean bulkAllowed;

}
