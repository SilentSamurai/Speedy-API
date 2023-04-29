package com.github.silent.samurai.speedy;

import com.github.silent.samurai.SpeedyLexer;
import com.github.silent.samurai.SpeedyParser;
import com.github.silent.samurai.speedy.models.AntlrRequest;
import com.github.silent.samurai.speedy.utils.StringUtils;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class AntlrParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(AntlrParser.class);

    final String url;

    public AntlrParser(String url) {
        this.url = url;
    }

    public AntlrRequest parse() throws UnsupportedEncodingException {
        String input = URLDecoder.decode(url, StandardCharsets.UTF_8.name());
        input = StringUtils.removeSpaces(input);
        LOGGER.info("input parsed {}", input);
        SpeedyLexer java8Lexer = new SpeedyLexer(CharStreams.fromString(input));

        CommonTokenStream tokens = new CommonTokenStream(java8Lexer);
        SpeedyParser parser = new SpeedyParser(tokens);
        ParseTreeWalker walker = new ParseTreeWalker();

        RequestListener listener = new RequestListener();
        walker.walk(listener, parser.request());

        if (parser.getNumberOfSyntaxErrors() > 0) {
            throw new RuntimeException("Failed to parse the url");
        }
        return listener.getEntries().get(0);
    }
}
