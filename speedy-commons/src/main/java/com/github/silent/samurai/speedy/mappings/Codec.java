package com.github.silent.samurai.speedy.mappings;

import com.github.silent.samurai.speedy.interfaces.SpeedyValue;

import java.util.function.Function;

public record Codec(
        Function<SpeedyValue, Object> encode,
        Function<Object, SpeedyValue> decode) {
}
