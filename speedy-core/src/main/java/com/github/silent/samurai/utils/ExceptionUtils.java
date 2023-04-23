package com.github.silent.samurai.utils;

import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.exceptions.NotFoundException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;

import javax.persistence.PersistenceException;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class ExceptionUtils {

    public static Map<String, Integer> exceptionToStatusMap = new HashMap<>();

    static {
        exceptionToStatusMap.put(NotFoundException.class.getName(), HttpServletResponse.SC_NOT_FOUND);
        exceptionToStatusMap.put(BadRequestException.class.getName(), HttpServletResponse.SC_BAD_REQUEST);
        exceptionToStatusMap.put(ConstraintViolationException.class.getName(), HttpServletResponse.SC_BAD_REQUEST);
        exceptionToStatusMap.put(DataException.class.getName(), HttpServletResponse.SC_BAD_REQUEST);

    }

    public static int getStatusFromException(Exception e) {
        if (e instanceof PersistenceException && e.getCause() != null) {
            return exceptionToStatusMap.getOrDefault(e.getCause().getClass().getName(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return exceptionToStatusMap.getOrDefault(e.getClass().getName(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
