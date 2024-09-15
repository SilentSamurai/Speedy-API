package com.github.silent.samurai.speedy.file.impl.metadata;

import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import lombok.Getter;
import org.checkerframework.checker.units.qual.K;

@Getter
public class FileKeyFieldMetadata extends FileFieldMetadata implements KeyFieldMetadata {

    boolean isKeyField = true;

    @Override
    public boolean shouldGenerateKey() {
        return true;
    }
}
