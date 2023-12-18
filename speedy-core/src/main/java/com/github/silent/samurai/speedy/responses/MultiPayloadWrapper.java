package com.github.silent.samurai.speedy.responses;

import com.github.silent.samurai.speedy.interfaces.MultiPayload;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MultiPayloadWrapper implements MultiPayload {

    private final List<? extends SpeedyValue> payload;
    private long pageIndex = 0;
    private long pageSize;
    private long totalPageCount = 1;

    public MultiPayloadWrapper(List<? extends SpeedyValue> payload) {
        this.payload = payload;
        this.pageSize = payload.size();
    }

    public static MultiPayloadWrapper wrapperInResponse(List<? extends SpeedyValue> payload) {
        return new MultiPayloadWrapper(payload);
    }

}
