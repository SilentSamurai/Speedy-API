package com.github.silent.samurai.utils;

import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.exceptions.ResourceNotFoundException;

import javax.servlet.http.HttpServletResponse;

public class ExceptionUtils {

    public static int getStatusFromException(Exception e) {
        if (e instanceof ResourceNotFoundException) {
            return HttpServletResponse.SC_NOT_FOUND;
        } else if (e instanceof BadRequestException) {
            return HttpServletResponse.SC_BAD_REQUEST;
        }
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }
}
