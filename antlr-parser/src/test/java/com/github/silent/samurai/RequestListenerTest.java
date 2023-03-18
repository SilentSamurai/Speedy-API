package com.github.silent.samurai;


import com.github.silent.samurai.utils.StringUtils;
import com.github.silent.samurai.utils.TreeUtils;
import com.google.gson.Gson;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RequestListenerTest {

    Logger logger = LogManager.getLogger(RequestListenerTest.class);

    @BeforeEach
    void setUp() {
    }

    Request parse(String input) {
        input = StringUtils.removeSpaces(input);
        logger.info("input {}", input);
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
    void testSingle() {
        String input = "/Customer(id='1'&name='jolly')?happy='holi'";
        Request request = parse(input);
        logger.info("request {}", new Gson().toJson(request));

    }


    @Test
    void getEntries() {
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
                "/Customer(id='1',name='jolly')?happy='holi'"
        };

        for (String input : inputEntries) {
            Request request = parse(input);
            logger.info("request {}", new Gson().toJson(request));
            assertEquals("Customer", request.getResource());
            logger.info("");
        }

    }
}