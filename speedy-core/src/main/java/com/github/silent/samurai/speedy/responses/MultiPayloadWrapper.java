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
    private int pageIndex = 0;
    private int pageCount = 1;

    public MultiPayloadWrapper(List<? extends SpeedyValue> payload) {
        this.payload = payload;
    }

    public static MultiPayloadWrapper wrapperInResponse(List<? extends SpeedyValue> payload) {
        return new MultiPayloadWrapper(payload);
    }

}
