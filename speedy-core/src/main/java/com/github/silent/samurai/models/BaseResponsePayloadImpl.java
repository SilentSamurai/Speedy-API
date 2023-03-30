package com.github.silent.samurai.models;

import com.github.silent.samurai.interfaces.IBaseResponsePayload;
import lombok.Data;

@Data
public class BaseResponsePayloadImpl implements IBaseResponsePayload {

    private Object payload;
    private int pageIndex;
    private int pageCount;

}
