package com.github.silent.samurai.speedy.utils;

public class StringUtils {

    public static String removeSpaces(String content) {
        StringBuilder sb = new StringBuilder();
        Character previous = null;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            char p = i - 1 >= 0 ? content.charAt(i - 1) : content.charAt(0);
            if (c == '\'' || c == '"') {
                if (previous == null) {
                    previous = c;
                }
                else if (previous == c && p != '\\') {
                    previous = null;
                }
            }

            if (c == ' ' && previous == null) continue;
            sb.append(c);
        }
        return sb.toString();
    }
}
