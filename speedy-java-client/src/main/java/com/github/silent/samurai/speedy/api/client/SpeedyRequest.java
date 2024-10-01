package com.github.silent.samurai.speedy.api.client;

import com.github.silent.samurai.speedy.api.client.builder.SpeedyCreateRequestBuilder;
import com.github.silent.samurai.speedy.api.client.builder.SpeedyDeleteRequestBuilder;
import com.github.silent.samurai.speedy.api.client.builder.SpeedyGetRequestBuilder;
import com.github.silent.samurai.speedy.api.client.builder.SpeedyUpdateRequestBuilder;

public class SpeedyRequest {

    public static SpeedyUpdateRequestBuilder update(String entityName) {
        return new SpeedyUpdateRequestBuilder(entityName);
    }

    public static SpeedyGetRequestBuilder get(String entityName) {
        return new SpeedyGetRequestBuilder(entityName);
    }

    public static SpeedyDeleteRequestBuilder delete(String entityName) {
        return new SpeedyDeleteRequestBuilder(entityName);
    }

    public static SpeedyCreateRequestBuilder create(String entityName) {
        return new SpeedyCreateRequestBuilder(entityName);
    }

    public static SpeedyQuery query(String entityName) {
        return SpeedyQuery.builder(entityName);
    }

    public static SpeedyQuery query() {
        return SpeedyQuery.builder();
    }

}
