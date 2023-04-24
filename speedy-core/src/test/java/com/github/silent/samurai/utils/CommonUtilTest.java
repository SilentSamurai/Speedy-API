package com.github.silent.samurai.utils;

import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommonUtilTest {

    @BeforeEach
    void setUp() {
    }

    @Test
    void gsonToType() {
        String input = "New Category";
        String output = CommonUtil.gsonToType(new JsonPrimitive(input), String.class);
        Assertions.assertEquals(input, output);
    }

    @Test
    void stringToType() {
        String input = "aisdiohaoisd-asdasd-asf";
        String output = CommonUtil.stringToPrimitive(input, String.class);
        Assertions.assertEquals(input, output);
    }

    @Test
    void stringToType2() {
        String input = "New Category";
        String output = CommonUtil.stringToPrimitive(input, String.class);
        Assertions.assertEquals(input, output);
    }
}