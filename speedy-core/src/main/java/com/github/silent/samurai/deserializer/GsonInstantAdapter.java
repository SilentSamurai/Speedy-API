package com.github.silent.samurai.deserializer;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class GsonInstantAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {

    private static final DateTimeFormatter INSTANT_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    @Override
    public JsonElement serialize(Instant instant, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(INSTANT_FORMATTER.format(instant));
    }

    @Override
    public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonPrimitive primitive = json.getAsJsonPrimitive();
        if (primitive.isString()) {
            return INSTANT_FORMATTER.parse(primitive.getAsString(), Instant::from);
        } else if (primitive.isNumber()) {
            long epochSeconds = (long) primitive.getAsDouble();
            int nanoAdjustment = (int) ((primitive.getAsLong() - epochSeconds) * 1_000_000_000L);
            return Instant.ofEpochSecond(epochSeconds, nanoAdjustment);
        } else {
            throw new JsonParseException("Date format not correct " + primitive);
        }
    }
}
