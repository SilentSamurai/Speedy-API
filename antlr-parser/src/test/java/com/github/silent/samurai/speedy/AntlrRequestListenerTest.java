package com.github.silent.samurai.speedy;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.silent.samurai.speedy.models.AntlrRequest;
import com.github.silent.samurai.speedy.models.ResourceRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.List;

class AntlrRequestListenerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AntlrRequestListenerTest.class);

    @BeforeEach
    void setUp() {
    }

    @Disabled
    @Test
    void testSingle() throws JsonProcessingException, UnsupportedEncodingException {
        String input = "/Customer?happy='holi'&metadata='hpo'";
        AntlrRequest parseTree = new AntlrParser(input).parse();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        LOGGER.info("request {}", objectMapper.writeValueAsString(parseTree));
    }


    @Test
    void getEntries() throws JsonProcessingException, UnsupportedEncodingException {
        String[] inputEntries = {
                "/Customer",
                "/Customer/",
                "/Customer(id='1', name='jolly')",
                "/Customer(id='1'& name='jolly')",
                "/Customer(id='1'| name='jolly')",
                "/Customer('1', 'joli')",
                "/Customer('1', 'joli?k$')?happy='holi'",
                "/Customer(id='1',name='jolly')?happy='holi'",
                "/Customer(amount < 0)",
                "/Customer(amount > 0 , amount < 100)",
                "/Customer(amount <> [1,2,3])",
                "/Customer(amount <!> [2,3,4])",
                "/Customer(id='1', name='jolly')?orderBy=['name','id']&orderByDesc='obc'",
                "/Customer(name <> ['name','id'], amount < 9)?orderBy=['name','id']&orderByDesc='obc'",
                "/Customer/Product",
                "/Customer(amount > 0)/Product",
                "/Customer(amount > 0)/Product( nonNullableField != 'null' )",
        };

        for (String input : inputEntries) {
            AntlrRequest antlrRequest = new AntlrParser(input).parse();
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            LOGGER.info("request {}", objectMapper.writeValueAsString(antlrRequest));
            List<ResourceRequest> requestList = antlrRequest.getRequestList();
            Assertions.assertEquals("Customer", requestList.get(0).getResource());
            if (requestList.size() > 1) {
                Assertions.assertEquals("Product", requestList.get(1).getResource());
            }
            LOGGER.info("");
        }

    }
}