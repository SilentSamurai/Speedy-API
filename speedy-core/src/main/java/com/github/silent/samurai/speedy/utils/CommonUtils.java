package com.github.silent.samurai.speedy.utils;

import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class CommonUtils {


    public static String getRequestURI(HttpServletRequest request) throws UnsupportedEncodingException {
        String requestURI = URLDecoder.decode(request.getRequestURI(), StandardCharsets.UTF_8);
        if (request.getQueryString() != null) {
            requestURI += "?" + URLDecoder.decode(request.getQueryString(), StandardCharsets.UTF_8);
        }
        return requestURI.replaceAll(SpeedyConstant.URI, "");
    }

}
