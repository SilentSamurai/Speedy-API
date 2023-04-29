package com.github.silent.samurai.speedy.utils;

import org.springframework.util.ClassUtils;

import java.util.Objects;

public class TypeUtils {

    public static boolean isPrimaryType(Class<?> type) {
        return ClassUtils.isPrimitiveOrWrapper(type) || Objects.equals(type, String.class);
    }
}
