package com.github.silent.samurai.speedy.api.client.models;

import com.github.silent.samurai.speedy.api.client.SpeedyApi;
import com.github.silent.samurai.speedy.api.client.SpeedyQuery;

public class SpeedyQueryRequest<T> {

    private final SpeedyQuery speedyQuery;
    private final SpeedyApi<T> speedyApi;

    public SpeedyQueryRequest(SpeedyQuery speedyQuery, SpeedyApi<T> speedyApi) {
        this.speedyQuery = speedyQuery;
        this.speedyApi = speedyApi;
    }

    public T execute() throws Exception {
        return speedyApi.query(speedyQuery);
    }

}
