package com.github.silent.samurai.speedy.helper;

import com.fasterxml.jackson.core.JsonProcessingException;

public class SpdyQ {

    public static SpdyQBuilder builder() {
        return new SpdyQBuilder();
    }

    public static SpdyQBuilder whereEq(String key, Object value) throws JsonProcessingException {
        return builder()
                .$where(key, SpdyQBuilder.$eq(value));
    }

}
