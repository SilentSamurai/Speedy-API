package com.github.silent.samurai;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.silent.samurai.speedy.model.AntlrRequest;
import com.github.silent.samurai.utils.StringUtils;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AntlrRequestListenerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AntlrRequestListenerTest.class);

    @BeforeEach
    void setUp() {
    }

    AntlrRequest parse(String input) {
        input = StringUtils.removeSpaces(input);
        LOGGER.info("input {}", input);
        SpeedyLexer java8Lexer = new SpeedyLexer(CharStreams.fromString(input));

        CommonTokenStream tokens = new CommonTokenStream(java8Lexer);
        SpeedyParser parser = new SpeedyParser(tokens);
        ParseTreeWalker walker = new ParseTreeWalker();

        RequestListener listener = new RequestListener();
        walker.walk(listener, parser.request());

//        List<String> ruleNamesList = Arrays.asList(parser.getRuleNames());
//        logger.info(TreeUtils.toPrettyTree(parser.request(), ruleNamesList));
        return listener.getEntries().get(0);
    }

    @Disabled
    @Test
    void testSingle() throws JsonProcessingException {
        String input = "/Customer?happy='holi'&metadata='hpo'";
        AntlrRequest antlrRequest = parse(input);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        LOGGER.info("request {}", objectMapper.writeValueAsString(antlrRequest));
    }


    @Test
    void getEntries() throws JsonProcessingException {
        String[] inputEntries = {
                "/Customer",
                "/Customer/",
                "/Customer(id='1', name='jolly')",
                "/Customer(id='1'& name='jolly')",
                "/Customer(id='1'| name='jolly')",
                "/Customer('1', 'joli')",
                "/Customer('1'& 'joli')",
                "/Customer('1'| 'joli')",
                "/Customer('1', 'joli?k$')?happy='holi'",
                "/Customer(id='1',name='jolly')?happy='holi'",
                "/Customer(amount < 0)",
                "/Customer(amount > 0 , amount < 100)",
                "/Customer(id='1', name='jolly')?orderBy=['name','id']&orderByDesc='obc'",
                "/Customer(name = ['name','id'], amount < 9)?orderBy=['name','id']&orderByDesc='obc'"
        };

        for (String input : inputEntries) {
            AntlrRequest antlrRequest = parse(input);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            LOGGER.info("request {}", objectMapper.writeValueAsString(antlrRequest));
            assertEquals("Customer", antlrRequest.getResource());
            LOGGER.info("");
        }

    }
}