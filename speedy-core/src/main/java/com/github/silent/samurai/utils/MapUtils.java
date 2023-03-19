package com.github.silent.samurai.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.Optional;

public class MapUtils {

    public static <K, V> V findAnyValueInMap(Map<K, V> map) {
        Optional<V> any = map.values().stream().findAny();
        return any.orElse(null);
    }

    public static <V> V findAnyValueInJsonObject(JsonObject jsonObject, Class<V> type) {
        Optional<Map.Entry<String, JsonElement>> any = jsonObject.entrySet().stream().findAny();
        if (any.isPresent()) {
            return CommonUtil.getGson().fromJson(any.get().getValue().getAsJsonObject(), type);
        }
        return null;
    }
}
