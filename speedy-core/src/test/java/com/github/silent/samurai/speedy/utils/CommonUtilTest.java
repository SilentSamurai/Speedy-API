package com.github.silent.samurai.speedy.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.mappings.TypeConverterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

class CommonUtilTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonUtilTest.class);


    @BeforeEach
    void setUp() {
    }

    @Test
    void stringTest() throws JsonProcessingException {
        String input = "New Category";
        JsonNode jsonNode = TextNode.valueOf(input);
        String output = CommonUtil.jsonToType(jsonNode, String.class);
        Assertions.assertEquals(input, output);
    }

    @Test
    void InstantTest() throws JsonProcessingException {
        String dateTime = "1984-01-01T02:30:00.000+00:00";
        //  1970-01-01T02:30:00.000+00:00
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        Instant input = Instant.from(dateTimeFormatter.parse(dateTime));
        JsonNode jsonNode = TextNode.valueOf(dateTime);
        Instant output = CommonUtil.jsonToType(jsonNode, Instant.class);
        LOGGER.info("output {}", output);
        Assertions.assertEquals(input, output);
    }

    @Test
    void localDateTest() throws JsonProcessingException {
        String dateTime = "1984-01-01";
        //  1970-01-01T02:30:00.000+00:00
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE;
        LocalDate input = LocalDate.from(dateTimeFormatter.parse(dateTime));
        JsonNode jsonNode = TextNode.valueOf(dateTime);
        LocalDate output = CommonUtil.jsonToType(jsonNode, LocalDate.class);
        LOGGER.info("output {}", output);
        Assertions.assertEquals(input, output);
    }

    @Test
    void dateTest() throws JsonProcessingException, ParseException {
        String dateTime = "1984-01-01 12:34:56";
        String dateTime2 = "1984-01-01T12:34:56";
        //  1970-01-01T02:30:00.000+00:00
        DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date input = simpleDateFormat.parse(dateTime);
        JsonNode jsonNode = TextNode.valueOf(dateTime2);
        Date output = CommonUtil.jsonToType(jsonNode, Date.class);
        LOGGER.info("output {}", output.toInstant().atZone(ZoneOffset.UTC));
        Assertions.assertEquals(input.toInstant().atOffset(ZoneOffset.UTC).toLocalDate(),
                output.toInstant().atZone(ZoneOffset.UTC).toLocalDate());
    }

    @Test
    void stringToType() throws SpeedyHttpException {
        String input = "aisdiohaoisd-asdasd-asf";
        String output = TypeConverterRegistry.fromString(input, String.class);
        Assertions.assertEquals(input, output);
    }

    @Test
    void stringToType2() throws SpeedyHttpException {
        String input = "New Category";
        String output = TypeConverterRegistry.fromString(input, String.class);
        Assertions.assertEquals(input, output);
    }
}