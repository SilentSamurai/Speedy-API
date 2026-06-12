package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.request.RequestContext;
import com.github.silent.samurai.speedy.request.SpeedyRequest;
import org.springframework.http.HttpMethod;

/// Determines the request operation type from HTTP method and URI action suffix.
///
/// Reads the action suffix (resolved by SpeedyUriContext) and HTTP method to
/// classify the request as GET_LIST, QUERY, CREATE, UPDATE, or DELETE. Sets
/// SpeedyRequestType on the context for consumption by SpeedyFactory's
/// dispatch switch.
///
/// @see SpeedyRequestType
public class OperationResolverHandler implements Handler {

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        SpeedyRequest request = context.getRequest();
        HttpMethod method = request.getHttpMethod();
        String actionSuffix = request.getActionSuffix();
        SpeedyEndpoint endpoint = SpeedyEndpoint.fromSuffix(actionSuffix);

        SpeedyRequestType requestType;

        if (method.equals(HttpMethod.GET)) {
            requestType = SpeedyRequestType.GET_LIST;
        } else if (method.equals(HttpMethod.POST)) {
            if (SpeedyEndpoint.QUERY == endpoint) {
                requestType = SpeedyRequestType.QUERY;
            } else if (SpeedyEndpoint.CREATE == endpoint) {
                requestType = SpeedyRequestType.CREATE;
            } else {
                throw new BadRequestException("not a valid request");
            }
        } else if (method.equals(HttpMethod.PUT) || method.equals(HttpMethod.PATCH)) {
            requestType = SpeedyRequestType.UPDATE;
        } else if (method.equals(HttpMethod.DELETE)) {
            requestType = SpeedyRequestType.DELETE;
        } else {
            throw new BadRequestException("not a valid request");
        }

        context.setRequestType(requestType);
    }
}
