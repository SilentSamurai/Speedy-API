package com.github.silent.samurai.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.Optional;

public class MapUtils {

    public static Object findAnyValueInMap(Map<String, String> map, Class<?> keyClass) {
        Optional<String> any = map.values().stream().findAny();
        if (any.isPresent()) {
            return CommonUtil.stringToPrimitive(any.get(), keyClass);
        }
        return null;
    }

    public static Object findAnyValueInJsonObject(JsonObject jsonObject, Class<?> type) {
        Optional<Map.Entry<String, JsonElement>> any = jsonObject.entrySet().stream().findAny();
        if (any.isPresent()) {
            return CommonUtil.gsonToType(any.get().getValue(), type);
        }
        return null;
    }
}
