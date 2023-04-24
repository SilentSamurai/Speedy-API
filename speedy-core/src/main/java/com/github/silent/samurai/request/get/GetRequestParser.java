package com.github.silent.samurai.request.get;

import com.github.silent.samurai.parser.SpeedyUriParser;

public class GetRequestParser {

    private final GetRequestContext context;

    public GetRequestParser(GetRequestContext context) {
        this.context = context;
    }

    public void process() throws Exception {
        SpeedyUriParser parser = new SpeedyUriParser(context.getMetaModelProcessor(), context.getRequestURI());
        parser.parse();
        context.setParser(parser);
    }

}
