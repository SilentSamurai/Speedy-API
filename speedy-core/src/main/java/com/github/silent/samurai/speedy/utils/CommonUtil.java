package com.github.silent.samurai.speedy.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.serializers.basic.BasicDeserializer;
import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonUtil {

    private static final ModelMapper modelMapper;

    static {
        modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setPropertyCondition(Conditions.isNotNull())
                .setMatchingStrategy(MatchingStrategies.STRICT);
    }

    public static <D, T> D mapModel(final Object subject, Class<D> tClass) {
        return modelMapper.map(subject, tClass);
    }

//    public static <D, T> List<D> mapModels(final Collection<T> entityList, Class<D> outCLass) {
//        return entityList.stream()
//                .map(entity -> mapModel(entity, outCLass))
//                .collect(Collectors.toList());
//    }
//
//    public static <S, D> D mapModel(final S source, D destination) {
//        modelMapper.map(source, destination);
//        return destination;
//    }

    public static String extractPhoneNumber(String phoneNumber) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < phoneNumber.length(); i++) {
            if (Character.isDigit(phoneNumber.charAt(i))) {
                sb.append(phoneNumber.charAt(i));
            }
        }
        if (sb.length() < 10) {
            return null;
        }
        return sb.substring(sb.length() - 10, sb.length());
    }

    public static String findRegexGroup(String pattern, String source, int group) {
        Pattern p = Pattern.compile(pattern);
        Matcher matcher = p.matcher(source);
        if (!matcher.matches()) {
            return null;
        }
        return matcher.group(group);
    }

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

    public static <D> D mapToModel(final Map<String, ?> map, Class<D> type) {
        return json().convertValue(map, type);
    }


    public static <T> T jsonToType(JsonNode jsonNode, Class<T> type) throws JsonProcessingException {
        return standardMapper.treeToValue(jsonNode, type);
    }

    public static <T> T quotedStringToPrimitive(String value, Class<T> type) throws BadRequestException {
        try {
            value = value.replaceAll("['|\"]", "");
            Object obj = stringToBasic(value, type);
            if (obj != null && isAssignableClass(obj.getClass(), type)) {
                return (T) obj;
            }
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
        return null;
    }

    public static <T> T stringToPrimitive(String value, Class<T> type) throws BadRequestException {
        try {
            Object obj = stringToBasic(value, type);
            if (obj != null && isAssignableClass(obj.getClass(), type)) {
                return (T) obj;
            }
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
        return null;
    }

    public static Object stringToBasic(String value, Class<?> targetType) throws Exception {
        return BasicDeserializer.deserialize(value, targetType);
    }

    public static List<String> inQuotesSplitter(String input, String regex) {
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
}
