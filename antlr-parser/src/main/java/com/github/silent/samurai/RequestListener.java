package com.github.silent.samurai;


import java.util.ArrayList;
import java.util.List;

public class RequestListener extends SpeedyBaseListener {

    private final List<AntlrRequest> antlrRequests = new ArrayList<>();
    private AntlrRequest current;

    @Override
    public void enterRequest(SpeedyParser.RequestContext ctx) {
        current = new AntlrRequest();
    }

    @Override
    public void exitRequest(SpeedyParser.RequestContext ctx) {
        antlrRequests.add(current);
    }

    @Override
    public void enterResource(SpeedyParser.ResourceContext ctx) {
        current.setResource(ctx.getText());
    }

    @Override
    public void enterArgument(SpeedyParser.ArgumentContext ctx) {
        current.getArguments().add(ctx.valString().getText());
    }

    @Override
    public void enterKeywordsParams(SpeedyParser.KeywordsParamsContext ctx) {
        current.getKeywords().put(ctx.paramKey().identifier().getText(), ctx.paramValue().valString().getText());
    }

    @Override
    public void enterSearchParameter(SpeedyParser.SearchParameterContext ctx) {
        if (ctx.paramValue() != null) {
            current.getQuery().add(ctx.identifier().getText(), ctx.paramValue().getText());
        } else {
            current.getQuery().add(ctx.identifier().getText(), String.valueOf(true));
        }
    }

    public List<AntlrRequest> getEntries() {
        return antlrRequests;
    }
}
