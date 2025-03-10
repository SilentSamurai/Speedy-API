package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.Handler;
import com.github.silent.samurai.speedy.interfaces.RequestContext;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;

public class RequestParserHandler implements Handler {

    private final Handler next;

    public RequestParserHandler(final Handler next) {
        this.next = next;
    }

    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        HttpServletRequest request = context.getHttpServletRequest();
        String requestURI = CommonUtil.getRequestURI(request);
        String method = request.getMethod();
        context.setRequestUri(requestURI);
        HttpMethod httpMethod = HttpMethod.valueOf(method);
        context.setHttpMethod(httpMethod);

        next.process(context);
    }

}
