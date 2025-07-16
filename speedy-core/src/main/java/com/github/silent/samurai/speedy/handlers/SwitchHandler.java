package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.request.RequestContext;
import org.springframework.http.HttpMethod;

public class SwitchHandler implements Handler {

    final Handler getRequestHandler;
    final Handler queryHandler;
    final Handler createHandler;
    final Handler updateHandler;
    final Handler deleteHandler;

    public SwitchHandler(Handler getHandler, Handler queryHandler, Handler createHandler, Handler updateHandler, Handler deleteHandler) {
        this.getRequestHandler = getHandler;
        this.queryHandler = queryHandler;
        this.createHandler = createHandler;
        this.updateHandler = updateHandler;
        this.deleteHandler = deleteHandler;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        HttpMethod method = context.getHttpMethod();
        String requestURI = context.getRequestUri();
        EntityMetadata entityMetadata = context.getEntityMetadata();

        if (method.equals(HttpMethod.GET)) {
            if (!entityMetadata.isReadAllowed()) {
                throw new BadRequestException(String.format("read not allowed for %s", entityMetadata.getName()));
            }
            getRequestHandler.process(context);
            return;
        } else if (method.equals(HttpMethod.POST)) {
            if (requestURI.contains("$query")) {
                if (!entityMetadata.isReadAllowed()) {
                    throw new BadRequestException(String.format("read not allowed for %s", entityMetadata.getName()));
                }
                queryHandler.process(context);
                return;
            } else if (requestURI.contains("$create")) {
                if (!entityMetadata.isCreateAllowed()) {
                    throw new BadRequestException(String.format("create not allowed for %s", entityMetadata.getName()));
                }
                createHandler.process(context);
                return;
            }
        } else if (method.equals(HttpMethod.PUT) || method.equals(HttpMethod.PATCH)) {
            if (requestURI.contains("$update")) {
                if (!entityMetadata.isUpdateAllowed()) {
                    throw new BadRequestException(String.format("update not allowed for %s", entityMetadata.getName()));
                }
                updateHandler.process(context);
                return;
            }
        } else if (method.equals(HttpMethod.DELETE)) {
            if (requestURI.contains("$delete")) {
                if (!entityMetadata.isDeleteAllowed()) {
                    throw new BadRequestException(String.format("delete not allowed for %s", entityMetadata.getName()));
                }
                deleteHandler.process(context);
                return;
            }
        }
        throw new BadRequestException("not a valid request");
    }
}
