package com.github.silent.samurai.speedy.request.get;

import com.github.silent.samurai.speedy.parser.SpeedyUriContext;

public class GetRequestParser {

    private final GetRequestContext context;

    public GetRequestParser(GetRequestContext context) {
        this.context = context;
    }

    public void process() throws Exception {
        SpeedyUriContext parser = new SpeedyUriContext(context.getMetaModelProcessor(), context.getRequestURI());
        parser.parse();
        context.setParser(parser);
    }


}
