package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;

import java.io.UnsupportedEncodingException;

public interface Handler {
    void process(RequestContext context) throws SpeedyHttpException;
}
