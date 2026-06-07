package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.request.RequestContext;

/// Routes requests by SpeedyRequestType to the appropriate CRUD handler.
///
/// Reads the operation type from the context (set by OperationResolverHandler)
/// and dispatches to GetHandler, QueryHandler, CreateHandler, UpdateHandler,
/// or DeleteHandler. Checks per-entity CRUD permissions before dispatching.
///
/// @see OperationResolverHandler
/// @see SpeedyRequestType
public class SwitchHandler implements Handler {

    final Handler getRequestHandler;
    final Handler queryHandler;
    final Handler createHandler;
    final Handler updateHandler;
    final Handler deleteHandler;

    public SwitchHandler(Handler getHandler, Handler queryHandler, Handler createHandler,
                         Handler updateHandler, Handler deleteHandler) {
        this.getRequestHandler = getHandler;
        this.queryHandler = queryHandler;
        this.createHandler = createHandler;
        this.updateHandler = updateHandler;
        this.deleteHandler = deleteHandler;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        SpeedyRequestType requestType = context.getRequestType();
        EntityMetadata entityMetadata = context.getEntityMetadata();

        switch (requestType) {
            case GET_LIST:
                if (!entityMetadata.isReadAllowed()) {
                    throw new BadRequestException(String.format("read not allowed for %s", entityMetadata.getName()));
                }
                getRequestHandler.process(context);
                return;
            case QUERY:
                if (!entityMetadata.isReadAllowed()) {
                    throw new BadRequestException(String.format("read not allowed for %s", entityMetadata.getName()));
                }
                queryHandler.process(context);
                return;
            case CREATE:
                if (!entityMetadata.isCreateAllowed()) {
                    throw new BadRequestException(String.format("create not allowed for %s", entityMetadata.getName()));
                }
                createHandler.process(context);
                return;
            case UPDATE:
                if (!entityMetadata.isUpdateAllowed()) {
                    throw new BadRequestException(String.format("update not allowed for %s", entityMetadata.getName()));
                }
                updateHandler.process(context);
                return;
            case DELETE:
                if (!entityMetadata.isDeleteAllowed()) {
                    throw new BadRequestException(String.format("delete not allowed for %s", entityMetadata.getName()));
                }
                deleteHandler.process(context);
                return;
        }
        throw new BadRequestException("not a valid request");
    }
}
