package com.github.silent.samurai.speedy.file.impl.metadata;

import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import lombok.Getter;

@Getter
public class FileKeyFieldMetadata extends FileFieldMetadata implements KeyFieldMetadata {

    boolean isKeyField = true;

    @Override
    public boolean shouldGenerateKey() {
        return true;
    }
}
