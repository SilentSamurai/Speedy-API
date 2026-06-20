package com.github.silent.samurai.speedy.jooq.impl.conversion;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyNull;

/// Leaf value codec for the jOOQ backend: converts between native JDBC column values and
/// {@link SpeedyValue}. This is the persistence-side analogue of {@code JsonResponseWriter.writeLeaf}
/// / {@code JsonStructureReader.readField} — it lives entirely inside the backend module, so the
/// format-agnostic walkers in {@code speedy-commons} only ever see {@link SpeedyValue}s and never
/// any backend-native column object.
public interface Converter {

    SpeedyValue toSpeedyValue(Object instance, FieldMetadata fieldMetadata) throws SpeedyHttpException;

    Object toColumnType(SpeedyValue instance, FieldMetadata fieldMetadata);

    default SpeedyValue speedyNull() {
        return SpeedyNull.SPEEDY_NULL;
    }

}
