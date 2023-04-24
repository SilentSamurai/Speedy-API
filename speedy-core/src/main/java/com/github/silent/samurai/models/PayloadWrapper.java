package com.github.silent.samurai.models;

import com.github.silent.samurai.interfaces.IBaseResponsePayload;
import lombok.Data;

@Data
public class PayloadWrapper implements IBaseResponsePayload {

    private final Object payload;
    private int pageIndex = 0;
    private int pageCount = 1;

    public static PayloadWrapper wrapperInResponse(Object payload) {
        return new PayloadWrapper(payload);
    }

}
