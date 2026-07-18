package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.enums.SpeedyHttpMethod;
import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;

/// Determines the request operation type from HTTP method and URI action suffix.
///
/// Reads the action suffix (resolved by SpeedyUriContext) and HTTP method to
/// classify the request as GET_LIST, QUERY, CREATE, UPDATE, REPLACE, or DELETE. Sets
/// SpeedyRequestType on the context for consumption by SpeedyFactory's
/// dispatch switch.
///
/// @see SpeedyRequestType
public class OperationResolverHandler implements com.github.silent.samurai.speedy.interfaces.Handler {

    @Override
    public void process(SpeedyContext context) throws SpeedyHttpException {
        SpeedyUriContext uriContext = context.get(SpeedyUriContext.class);
        SpeedyHttpMethod method = context.get(SpeedyHttpMethod.class);
        String actionSuffix = uriContext.getActionSuffix();
        SpeedyEndpoint endpoint = SpeedyEndpoint.fromSuffix(actionSuffix);

        SpeedyRequestType requestType;

        if (method == SpeedyHttpMethod.GET) {
            if (SpeedyEndpoint.METADATA == endpoint) {
                requestType = SpeedyRequestType.METADATA;
            } else {
                requestType = SpeedyRequestType.GET_LIST;
            }
        } else if (method == SpeedyHttpMethod.POST) {
            if (SpeedyEndpoint.QUERY == endpoint) {
                requestType = SpeedyRequestType.QUERY;
            } else if (SpeedyEndpoint.CREATE == endpoint) {
                requestType = SpeedyRequestType.CREATE;
            } else {
                throw new BadRequestException("not a valid request");
            }
        } else if (method == SpeedyHttpMethod.PUT) {
            // PUT = full replace; PATCH = partial update. They diverge in validation
            // (required-field enforcement) and column flatten (null-out of omitted fields).
            requestType = SpeedyRequestType.REPLACE;
        } else if (method == SpeedyHttpMethod.PATCH) {
            requestType = SpeedyRequestType.UPDATE;
        } else if (method == SpeedyHttpMethod.DELETE) {
            requestType = SpeedyRequestType.DELETE;
        } else {
            throw new BadRequestException("not a valid request");
        }

        context.put(requestType);
    }
}
