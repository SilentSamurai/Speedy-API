package com.github.silent.samurai.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommonUtil {

    private static final ModelMapper modelMapper;
    private static final ObjectMapper objectMapper;

    static {
        modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setPropertyCondition(Conditions.isNotNull())
                .setMatchingStrategy(MatchingStrategies.STRICT);
        objectMapper = new ObjectMapper();
    }

    public static <D> D mapToModel(final Map<String, ?> map, Class<D> type) {
        return objectMapper.convertValue(map, type);
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

    public static GsonBuilder gsonBuildr = new GsonBuilder();

    static {
        gsonBuildr.setDateFormat("yyyy-MM-dd hh:mm:ss.S");
    }

    public static Gson getGson() {
        return gsonBuildr.create();
    }

    public static Object gsonToType(JsonElement jsonElement, Class<?> type) {
        return gsonBuildr.create().fromJson(jsonElement, type);
    }

    public static Object stringToType(String value, Class<?> type) {
        return gsonBuildr.create().fromJson(value, type);
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
}
