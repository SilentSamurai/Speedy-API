package com.github.silent.samurai.speedy.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.silent.samurai.speedy.exceptions.ConversionException;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CommonUtil {


    private static final Jackson2ObjectMapperBuilder jacksonBuildr = new Jackson2ObjectMapperBuilder();
    private static final ObjectMapper standardMapper;

    static {
        jacksonBuildr.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        jacksonBuildr.featuresToEnable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
        standardMapper = jacksonBuildr.build();
    }

    public static ObjectMapper json() {
        return standardMapper;
    }

    public static String toJson(Object value) throws JsonProcessingException {
        return standardMapper.writeValueAsString(value);
    }

    public static <T> T jsonToType(JsonNode jsonNode, Class<T> type) throws JsonProcessingException {
        return standardMapper.treeToValue(jsonNode, type);
    }

    public static List<String> inQuotesSplitter(String input, String matches) {
        List<String> tokens = new ArrayList<>();
        int start = 0;
        boolean inQuotes = false;
        for (int current = 0; current < input.length(); current++) {
            if (input.charAt(current) == '\"') inQuotes = !inQuotes; // toggle state
            else if (input.charAt(current) == ',' && !inQuotes) {
                tokens.add(input.substring(start, current));
                start = current + 1;
            }
        }
        tokens.add(input.substring(start));
        return tokens;
    }

    public static boolean isAssignableClass(Class<?> fromClass, Class<?> toClass) {
        if (fromClass == null || toClass == null) {
            return false;
        }
        // Check for primitive types and wrappers
        if (fromClass.isPrimitive()) {
            if (toClass.isPrimitive()) {
                return fromClass == toClass;
            }
            if (toClass.isAssignableFrom(wrapperType(fromClass))) {
                return true;
            }
        } else if (toClass.isPrimitive()) {
            if (fromClass.isAssignableFrom(wrapperType(toClass))) {
                return true;
            }
        }
        return toClass.isAssignableFrom(fromClass);
    }

    private static Class<?> wrapperType(Class<?> primitiveType) {
        if (primitiveType == boolean.class) {
            return Boolean.class;
        } else if (primitiveType == byte.class) {
            return Byte.class;
        } else if (primitiveType == char.class) {
            return Character.class;
        } else if (primitiveType == short.class) {
            return Short.class;
        } else if (primitiveType == int.class) {
            return Integer.class;
        } else if (primitiveType == long.class) {
            return Long.class;
        } else if (primitiveType == float.class) {
            return Float.class;
        } else if (primitiveType == double.class) {
            return Double.class;
        }
        return null;
    }

    public static String getRequestURI(HttpServletRequest request) {
        String requestURI = URLDecoder.decode(request.getRequestURI(), StandardCharsets.UTF_8);
        if (request.getQueryString() != null) {
            requestURI += "?" + URLDecoder.decode(request.getQueryString(), StandardCharsets.UTF_8);
        }
        return requestURI.replaceAll(SpeedyConstant.URI, "");
    }

    public static String generateString(int length) {
        StringBuilder builder = new StringBuilder(length);
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * characters.length());
            builder.append(characters.charAt(index));
        }
        return builder.toString();
    }

    public static <T extends Enum<T>> T convertToEnum(Class<T> enumClass, SpeedyValue value) {
        if (value == null || value.isNull() || !enumClass.isEnum()) {
            throw new ConversionException(
                    "Cannot convert NULL to enum %s".formatted(enumClass.getSimpleName())
            );
        }

        return switch (value.getValueType()) {
            case INT, ENUM_ORD -> {
                Long ordinal = value.asInt();
                var constants = enumClass.getEnumConstants();
                if (ordinal < 0 || ordinal >= constants.length) {
                    throw new ConversionException(
                            "Invalid ordinal %d for enum %s".formatted(ordinal, enumClass.getSimpleName())
                    );
                }
                yield constants.length > ordinal ? constants[ordinal.intValue()] : constants[0];
            }
            case TEXT, ENUM -> {
                var enumName = value.asText();
                try {
                    yield Enum.valueOf(enumClass, enumName);
                } catch (IllegalArgumentException iae) {
                    // case-insensitive match
                    for (var constant : enumClass.getEnumConstants()) {
                        if (constant.name().equalsIgnoreCase(enumName)) {
                            yield constant;
                        }
                    }
                    throw new ConversionException(
                            "Cannot convert TEXT '%s' to enum %s"
                                    .formatted(enumName, enumClass.getSimpleName()), iae
                    );
                }
            }
            case BOOL, COLLECTION, OBJECT, FLOAT, DATE, TIME, DATE_TIME, ZONED_DATE_TIME, NULL ->
                    throw new ConversionException(
                            "Unsupported JSON type %s for enum %s"
                                    .formatted(value.getValueType(), enumClass.getSimpleName())
                    );
        };
    }


}
