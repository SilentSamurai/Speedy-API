package com.github.silent.samurai.speedy.file.impl.models;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public class JsonEntity {

    @NotNull
    public String name;

    public List<JsonField> fields;

    @NotNull
    public boolean hasCompositeKey;

    public String dbTable;
    public String keyType;

}
