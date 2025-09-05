package com.github.silent.samurai.speedy.interfaces.query;

import com.github.silent.samurai.speedy.enums.ColumnType;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyNull;

public interface Converter {

    SpeedyValue toSpeedyValue(Object instance, FieldMetadata fieldMetadata) throws SpeedyHttpException;

    Object toColumnType(SpeedyValue instance, FieldMetadata fieldMetadata);

    default SpeedyValue speedyNull() {
        return SpeedyNull.SPEEDY_NULL;
    }

}
