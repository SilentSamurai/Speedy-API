package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
public class SpeedyZonedDateTime implements SpeedyValue {

    private ZonedDateTime value;

    public SpeedyZonedDateTime(ZonedDateTime value) {
        this.value = value;
    }


    @Override
    public ValueType getValueType() {
        return ValueType.ZONED_DATE_TIME;
    }

    @Override
    public boolean isEmpty() {
        return value == null;
    }

    @Override
    public ZonedDateTime asZonedDateTime() {
        return value;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SpeedyZonedDateTime{");
        sb.append("value=").append(value);
        sb.append('}');
        return sb.toString();
    }
}
