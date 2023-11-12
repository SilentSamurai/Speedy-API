package com.github.silent.samurai.speedy.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.springframework.http.MediaType;

import javax.persistence.PersistenceException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
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

    public static void writeException(HttpServletResponse response, SpeedyHttpException e) throws IOException {
        response.setStatus(e.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ObjectMapper jsPr = CommonUtil.json();
        ObjectNode basePayload = jsPr.createObjectNode();
        basePayload.put("status", e.getStatus());
        basePayload.put("message", e.getLocalizedMessage());
        basePayload.put("timestamp", LocalDateTime.now().toString());
        jsPr.writeValue(response.getWriter(), basePayload);
    }
}
