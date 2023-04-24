package com.github.silent.samurai.utils;

import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.exceptions.NotFoundException;
import com.github.silent.samurai.exceptions.SpeedyHttpException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
        JsonObject basePayload = new JsonObject();
        basePayload.addProperty("status", e.getStatus());
        basePayload.addProperty("message", e.getLocalizedMessage());
        basePayload.addProperty("timestamp", LocalDateTime.now().toString());
        Gson gson = CommonUtil.getGson();
        gson.toJson(basePayload, response.getWriter());
    }
}
