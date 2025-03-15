package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.request.RequestContext;

public interface Handler {
    void process(RequestContext context) throws SpeedyHttpException;
}
