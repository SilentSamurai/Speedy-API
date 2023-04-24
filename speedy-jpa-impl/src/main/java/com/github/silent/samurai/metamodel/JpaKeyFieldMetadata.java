package com.github.silent.samurai.metamodel;

import com.github.silent.samurai.interfaces.KeyFieldMetadata;
import lombok.Data;


@Data
public class JpaKeyFieldMetadata extends JpaFieldMetadata implements KeyFieldMetadata {

    private boolean isId;

    @Override
    public boolean isKeyField() {
        return isId;
    }

}
