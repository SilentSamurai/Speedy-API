package com.github.silent.samurai.speedy.responses;

import com.github.silent.samurai.speedy.interfaces.SinglePayload;
import com.github.silent.samurai.speedy.models.SpeedyEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SinglePayloadWrapper implements SinglePayload {

    private final SpeedyEntity payload;
    private long pageIndex = 0;
    private long pageSize = 1;
    private long totalPageCount = 1;

    public SinglePayloadWrapper(SpeedyEntity payload) {
        this.payload = payload;
    }

    public static SinglePayloadWrapper wrapperInResponse(SpeedyEntity payload) {
        return new SinglePayloadWrapper(payload);
    }

}
