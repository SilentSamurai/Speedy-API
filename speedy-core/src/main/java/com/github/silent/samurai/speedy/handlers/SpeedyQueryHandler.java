package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.request.RequestContext;

public class SpeedyQueryHandler implements Handler {

    final Handler next;
    public SpeedyQueryHandler(Handler handler) {
        this.next = handler;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        next.process(context);
    }
}
