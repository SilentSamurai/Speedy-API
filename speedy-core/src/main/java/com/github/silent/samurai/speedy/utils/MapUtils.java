package com.github.silent.samurai.speedy.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.mappings.TypeConverterRegistry;

import java.util.Map;
import java.util.Optional;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class MapUtils {

    public static Object findAnyValueInMap(Map<String, String> map, Class<?> keyClass) throws SpeedyHttpException {
        Optional<String> any = map.values().stream().findAny();
        if (any.isPresent()) {
            return TypeConverterRegistry.fromString(any.get(), keyClass);
        }
        return null;
    }

    public static Object findAnyValueInJsonObject(JsonNode jsonNode, Class<?> type) throws JsonProcessingException {
        Stream<JsonNode> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(jsonNode.iterator(), 0), false);
        Optional<JsonNode> any = stream.findAny();
        if (any.isPresent()) {
            return CommonUtil.jsonToType(any.get(), type);
        }
        return null;
    }
}
