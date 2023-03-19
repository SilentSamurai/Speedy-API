package com.github.silent.samurai;

import com.github.silent.samurai.utils.StringUtils;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class AntlrParser {

    Logger logger = LogManager.getLogger(AntlrParser.class);

    final String url;

    public AntlrParser(String url) {
        this.url = url;
    }

    public Request parse() throws UnsupportedEncodingException {
        String input = URLDecoder.decode(url, StandardCharsets.UTF_8.name());
        input = StringUtils.removeSpaces(input);
        logger.info("input parsed {}", input);
        SpeedyLexer java8Lexer = new SpeedyLexer(CharStreams.fromString(input));

        CommonTokenStream tokens = new CommonTokenStream(java8Lexer);
        SpeedyParser parser = new SpeedyParser(tokens);
        ParseTreeWalker walker = new ParseTreeWalker();

        RequestListener listener = new RequestListener();
        walker.walk(listener, parser.request());
        if (listener.getEntries().isEmpty()) {
            throw new RuntimeException("Failed to parse the url");
        }
        return listener.getEntries().get(0);
    }
}
