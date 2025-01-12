package com.github.silent.samurai.speedy.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.mappings.String2JavaType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;

import java.util.Map;
import java.util.Optional;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class MapUtils {

    public static Object findAnyValueInMap(Map<String, String> map, Class<?> keyClass) throws BadRequestException {
        Optional<String> any = map.values().stream().findAny();
        if (any.isPresent()) {
            return String2JavaType.stringToPrimitive(any.get(), keyClass);
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
