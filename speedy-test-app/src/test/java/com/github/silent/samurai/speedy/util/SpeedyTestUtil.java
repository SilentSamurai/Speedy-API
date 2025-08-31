package com.github.silent.samurai.speedy.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.silent.samurai.speedy.api.client.models.SpeedyResponse;
import com.github.silent.samurai.speedy.api.client.utils.JsonUtil;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

public class SpeedyTestUtil {

    public static DocumentContext jsonPath(SpeedyResponse response) throws JsonProcessingException {
        return JsonPath.parse(JsonUtil.toJson(response));
    }
}
