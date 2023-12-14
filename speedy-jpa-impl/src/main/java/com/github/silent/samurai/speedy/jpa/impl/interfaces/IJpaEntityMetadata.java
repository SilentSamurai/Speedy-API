package com.github.silent.samurai.speedy.jpa.impl.interfaces;

import com.github.silent.samurai.speedy.interfaces.EntityMetadata;

public interface IJpaEntityMetadata extends EntityMetadata {

    Object createNewEntityInstance() throws Exception;

    Object createNewKeyInstance() throws Exception;
}
