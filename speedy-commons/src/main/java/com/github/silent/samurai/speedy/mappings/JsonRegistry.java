package com.github.silent.samurai.speedy.mappings;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.SpeedyBoolean;
import com.github.silent.samurai.speedy.models.SpeedyDate;
import com.github.silent.samurai.speedy.models.SpeedyDateTime;
import com.github.silent.samurai.speedy.models.SpeedyDouble;
import com.github.silent.samurai.speedy.models.SpeedyInt;
import com.github.silent.samurai.speedy.models.SpeedyText;
import com.github.silent.samurai.speedy.models.SpeedyTime;
import com.github.silent.samurai.speedy.models.SpeedyZonedDateTime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

public class JsonRegistry extends ConversionRegistry<ValueType> {

    public JsonRegistry(JsonRegistry parent) {
        super(parent);
    }

    public static JsonRegistry defaults() {
        JsonRegistry r = new JsonRegistry(null);

        r.register(ValueType.BOOL,
                sv -> ((SpeedyBoolean) sv).getValue(),
                raw -> {
                    if (raw instanceof Boolean b) return new SpeedyBoolean(b);
                    if (raw instanceof String s) return new SpeedyBoolean(Boolean.parseBoolean(s));
                    return new SpeedyBoolean(Boolean.parseBoolean(String.valueOf(raw)));
                });

        r.register(ValueType.TEXT,
                sv -> ((SpeedyText) sv).getValue(),
                raw -> new SpeedyText(String.valueOf(raw)));

        r.register(ValueType.INT,
                sv -> ((SpeedyInt) sv).getValue(),
                raw -> {
                    if (raw instanceof Number n) return new SpeedyInt(n.longValue());
                    return new SpeedyInt(Long.parseLong(String.valueOf(raw)));
                });

        r.register(ValueType.FLOAT,
                sv -> ((SpeedyDouble) sv).getValue(),
                raw -> {
                    if (raw instanceof Number n) return new SpeedyDouble(n.doubleValue());
                    return new SpeedyDouble(Double.parseDouble(String.valueOf(raw)));
                });

        r.register(ValueType.DATE,
                sv -> ((SpeedyDate) sv).getValue().format(DateTimeFormatter.ISO_DATE),
                raw -> {
                    String s = String.valueOf(raw);
                    return new SpeedyDate(LocalDate.parse(s, DateTimeFormatter.ISO_DATE));
                });

        r.register(ValueType.TIME,
                sv -> ((SpeedyTime) sv).getValue().format(DateTimeFormatter.ISO_TIME),
                raw -> {
                    String s = String.valueOf(raw);
                    return new SpeedyTime(LocalTime.parse(s, DateTimeFormatter.ISO_TIME));
                });

        r.register(ValueType.DATE_TIME,
                sv -> ((SpeedyDateTime) sv).getValue().format(DateTimeFormatter.ISO_DATE_TIME),
                raw -> {
                    String s = String.valueOf(raw);
                    return new SpeedyDateTime(LocalDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME));
                });

        r.register(ValueType.ZONED_DATE_TIME,
                sv -> ((SpeedyZonedDateTime) sv).asZonedDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                raw -> {
                    String s = String.valueOf(raw);
                    return new SpeedyZonedDateTime(ZonedDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                });

        r.register(ValueType.ENUM,
                sv -> sv.asEnum(),
                raw -> new SpeedyText(String.valueOf(raw)));

        r.register(ValueType.ENUM_ORD,
                sv -> sv.asEnumOrd(),
                raw -> {
                    long ord = raw instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(raw));
                    return new SpeedyInt(ord);
                });

        return r;
    }
}
