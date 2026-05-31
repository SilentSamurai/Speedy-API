package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.request.RequestContext;
import org.springframework.http.HttpMethod;

/// # SwitchHandler
///
/// Routes requests by HTTP method and URI suffix
/// ({@code $query}, {@code $create}, {@code $update}, {@code $delete}).
/// Checks CRUD permissions via {@code EntityMetadata.is{Read/Create/Update/Delete}Allowed()}
/// before dispatching to the appropriate CRUD handler.
///
/// ## Purpose
/// - Acts as the routing dispatcher for CRUD operations
/// - Enforces per-entity action permissions before processing
/// - Rejects invalid method/endpoint combinations with a 400 error
///
/// ## Routing Table
/// | Method      | Suffix  | Dispatches to  | Permission check    |
/// |-------------|---------|----------------|---------------------|
/// | GET         | (none)  | GetHandler     | isReadAllowed()     |
/// | POST        | $query  | QueryHandler   | isReadAllowed()     |
/// | POST        | $create | CreateHandler  | isCreateAllowed()   |
/// | PUT / PATCH | $update | UpdateHandler  | isUpdateAllowed()   |
/// | DELETE      | $delete | DeleteHandler  | isDeleteAllowed()   |
///
/// ## Chain Position
/// Dispatches to one of five CRUD handlers; does not call a next handler directly.
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
        EntityMetadata entityMetadata = context.getEntityMetadata();
        String lastPathSegment = context.getActionSuffix();
        SpeedyEndpoint endpoint = SpeedyEndpoint.fromSuffix(lastPathSegment);

        if (method.equals(HttpMethod.GET)) {
            if (!entityMetadata.isReadAllowed()) {
                throw new BadRequestException(String.format("read not allowed for %s", entityMetadata.getName()));
            }
            getRequestHandler.process(context);
            return;
        } else if (method.equals(HttpMethod.POST)) {
            if (SpeedyEndpoint.QUERY == endpoint) {
                if (!entityMetadata.isReadAllowed()) {
                    throw new BadRequestException(String.format("read not allowed for %s", entityMetadata.getName()));
                }
                queryHandler.process(context);
                return;
            } else if (SpeedyEndpoint.CREATE == endpoint) {
                if (!entityMetadata.isCreateAllowed()) {
                    throw new BadRequestException(String.format("create not allowed for %s", entityMetadata.getName()));
                }
                createHandler.process(context);
                return;
            }
        } else if (method.equals(HttpMethod.PUT) || method.equals(HttpMethod.PATCH)) {
            if (SpeedyEndpoint.UPDATE == endpoint) {
                if (!entityMetadata.isUpdateAllowed()) {
                    throw new BadRequestException(String.format("update not allowed for %s", entityMetadata.getName()));
                }
                updateHandler.process(context);
                return;
            }
        } else if (method.equals(HttpMethod.DELETE)) {
            if (SpeedyEndpoint.DELETE == endpoint) {
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
