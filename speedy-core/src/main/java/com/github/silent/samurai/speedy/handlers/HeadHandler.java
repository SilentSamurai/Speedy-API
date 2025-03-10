package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.RequestContext;
import jakarta.servlet.http.HttpServletRequest;

public class HeadHandler implements Handler {

    final Handler next;
    public HeadHandler(Handler handler) {
        this.next = handler;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        next.process(context);
    }
}
