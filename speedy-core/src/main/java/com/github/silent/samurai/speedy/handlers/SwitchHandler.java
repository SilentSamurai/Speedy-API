package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.RequestContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;

public class SwitchHandler implements Handler {

    final Handler getRequestHandler;
    final Handler queryHandler;
    final Handler createHandler;
    final Handler updateHandler;
    final Handler deleteHandler;

    public SwitchHandler(Handler getRequestHandler, Handler queryHandler, Handler createHandler, Handler updateHandler, Handler deleteHandler) {
        this.getRequestHandler = getRequestHandler;
        this.queryHandler = queryHandler;
        this.createHandler = createHandler;
        this.updateHandler = updateHandler;
        this.deleteHandler = deleteHandler;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        HttpMethod method = context.getHttpMethod();
        String requestURI = context.getRequestUri();
        if (method.equals(HttpMethod.GET)) {
            getRequestHandler.process(context);
            return;
        } else if (method.equals(HttpMethod.POST)) {
            if (requestURI.contains("$query")) {
                queryHandler.process(context);
                return;
            } else if (requestURI.contains("$create")) {
                createHandler.process(context);
                return;
            }
        } else if (method.equals(HttpMethod.PUT) || method.equals(HttpMethod.PATCH)) {
            if (requestURI.contains("$update")) {
                updateHandler.process(context);
                return;
            }
        } else if (method.equals(HttpMethod.DELETE)) {
            if (requestURI.contains("$delete")) {
                deleteHandler.process(context);
                return;
            }
        }
        throw new BadRequestException("not a valid request");
    }
}
